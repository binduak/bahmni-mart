package org.bahmni.mart.config.stepconfigurer;

import org.bahmni.mart.config.job.JobDefinitionReader;
import org.bahmni.mart.config.job.model.JobDefinition;
import org.bahmni.mart.exports.ObservationExportStep;
import org.bahmni.mart.exports.updatestrategy.IncrementalStrategyContext;
import org.bahmni.mart.exports.updatestrategy.IncrementalUpdateStrategy;
import org.bahmni.mart.form.FormListProcessor;
import org.bahmni.mart.form.domain.BahmniForm;
import org.bahmni.mart.form.service.ConceptService;
import org.bahmni.mart.table.FormTableMetadataGenerator;
import org.bahmni.mart.table.TableGeneratorStep;
import org.bahmni.mart.table.domain.TableData;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.bahmni.mart.config.job.JobDefinitionUtil.isAddMoreMultiSelectEnabled;

public abstract class StepConfigurer implements StepConfigurerContract {

    @Autowired
    protected TableGeneratorStep tableGeneratorStep;

    @Autowired
    protected FormTableMetadataGenerator formTableMetadataGenerator;

    @Autowired
    protected ObjectFactory<ObservationExportStep> observationExportStepFactory;

    @Autowired
    protected FormListProcessor formListProcessor;

    @Autowired
    protected JobDefinitionReader jobDefinitionReader;

    @Autowired
    private IncrementalStrategyContext incrementalStrategyContext;

    @Autowired
    protected ConceptService conceptService;
    private List<BahmniForm> allForms;

    @Override
    public void generateTableData(JobDefinition jobDefinition) {
        allForms = getAllForms();
        allForms.forEach(formTableMetadataGenerator::addMetadataForForm);
        allForms.forEach(form -> {
            if (!isAddMoreMultiSelectEnabled(jobDefinition)) {
                revokeConstraints(formTableMetadataGenerator.getTableData(form));
            }
        });
    }

    @Override
    public void createTables(JobDefinition jobDefinition) {
        tableGeneratorStep.createTables(formTableMetadataGenerator.getTableDataList(), jobDefinition);
    }

    @Override
    public void registerSteps(FlowBuilder<FlowJobBuilder> completeDataExport, JobDefinition jobDefinition) {
        for (BahmniForm form : allForms) {
            ObservationExportStep observationExportStep = observationExportStepFactory.getObject();
            observationExportStep.setJobDefinition(jobDefinition);
            observationExportStep.setForm(form);
            TableData tableData = formTableMetadataGenerator.getTableData(form);
            addSteps(completeDataExport, jobDefinition, observationExportStep, tableData);
        }
    }

    private void addSteps(FlowBuilder<FlowJobBuilder> completeDataExport, JobDefinition jobDefinition,
                          ObservationExportStep observationExportStep, TableData tableData) {
        if (nonNull(tableData) && isMetadataSame(jobDefinition, tableData))
            completeDataExport.next(observationExportStep.getRemovalStep());
        completeDataExport.next(observationExportStep.getStep());
    }

    private boolean isMetadataSame(JobDefinition jobDefinition, TableData tableData) {
        IncrementalUpdateStrategy updateStrategy = incrementalStrategyContext.getStrategy(jobDefinition.getType());
        return !updateStrategy.isMetaDataChanged(tableData.getName(), jobDefinition.getName());
    }

    private void revokeConstraints(TableData tableData) {
        if (isNull(tableData)) return;
        tableData.getColumns().forEach(tableColumn -> {
            if (tableColumn.isPrimaryKey()) {
                tableColumn.setPrimaryKey(false);
            }
            if (tableColumn.getReference() != null) {
                tableColumn.setReference(null);
            }
        });
    }

    protected abstract List<BahmniForm> getAllForms();
}
