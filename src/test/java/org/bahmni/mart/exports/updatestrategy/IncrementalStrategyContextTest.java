package org.bahmni.mart.exports.updatestrategy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.bahmni.mart.CommonTestHelper.setValuesForMemberFields;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class IncrementalStrategyContextTest {

    @Mock
    private EavIncrementalUpdateStrategy eavIncrementalUpdateStrategy;

    @Mock
    private SimpleIncrementalUpdateStrategy simpleIncrementalUpdateStrategy;

    @Mock
    private Form2ObsIncrementalStrategy form2ObsIncrementalStrategy;

    private Map<String, IncrementalUpdateStrategy> incrementalStrategies;

    @Test
    public void shouldGiveEavIncrementalUpdateStrategyIfJobTypeIsEAV() throws Exception {
        IncrementalStrategyContext incrementalStrategyContext = new IncrementalStrategyContext();
        incrementalStrategies = new HashMap<>();
        incrementalStrategies.put("EAV", eavIncrementalUpdateStrategy);

        setValuesForMemberFields(incrementalStrategyContext, "incrementalStrategies", incrementalStrategies);


        assertEquals(eavIncrementalUpdateStrategy, incrementalStrategyContext.getStrategy("eav"));
    }

    @Test
    public void shouldGiveForm2ObsIncrementalUpdateStrategyIfJobTypeIsForm2Obs() throws Exception {
        IncrementalStrategyContext incrementalStrategyContext = new IncrementalStrategyContext();
        incrementalStrategies = new HashMap<>();
        incrementalStrategies.put("FORM2OBS", form2ObsIncrementalStrategy);

        setValuesForMemberFields(incrementalStrategyContext, "incrementalStrategies", incrementalStrategies);


        assertEquals(form2ObsIncrementalStrategy, incrementalStrategyContext.getStrategy("form2obs"));
    }

    @Test
    public void shouldGiveSimpleIncrementalUpdateStrategyIfJobTypeDoesNotMatch() throws Exception {
        IncrementalStrategyContext incrementalStrategyContext = new IncrementalStrategyContext();
        incrementalStrategies = new HashMap<>();

        setValuesForMemberFields(incrementalStrategyContext, "simpleIncrementalUpdateStrategy",
                simpleIncrementalUpdateStrategy);
        setValuesForMemberFields(incrementalStrategyContext, "incrementalStrategies", incrementalStrategies);


        assertEquals(simpleIncrementalUpdateStrategy, incrementalStrategyContext.getStrategy("someJobType"));
    }
}
