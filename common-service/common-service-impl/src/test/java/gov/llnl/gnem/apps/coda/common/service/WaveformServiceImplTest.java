package gov.llnl.gnem.apps.coda.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.test.annotations.IntTest;
import gov.llnl.gnem.apps.coda.common.repository.WaveformRepository;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.impl.WaveformServiceImpl;

@IntTest
@DataJpaTest(showSql = false)
@ContextConfiguration(classes = CommonServiceTestContext.class)
public class WaveformServiceImplTest {

    @Autowired
    private WaveformRepository waveformRepository;

    @Autowired
    private WaveformServiceImpl waveformService;

    @Autowired
    @Mock
    private NotificationService notificationService;

    private static final Date zeroDate = Date.from(Instant.EPOCH);

    @Test
    public void testSaveDuplicateWaveform() throws Exception {
        Waveform initialWaveform = genWaveform();
        waveformService.save(new Waveform().mergeNonNullOrEmptyFields(initialWaveform));
        waveformService.save(new Waveform().mergeNonNullOrEmptyFields(initialWaveform));
        assertThat(waveformService.findAll()).size().isEqualTo(1).describedAs("Should have saved only one waveform entry");
    }

    @Test
    public void testSaveWaveformWithChangedPicks() throws Exception {
        Waveform initialWaveform = genWaveform();
        Waveform firstWaveform = waveformService.save(new Waveform().mergeNonNullOrEmptyFields(initialWaveform));
        Waveform secondWaveform = waveformService.findOneForUpdate(firstWaveform.getId());

        secondWaveform.setAssociatedPicks(
                secondWaveform.getAssociatedPicks().stream().map(p -> new WaveformPick().mergeNonNullOrEmptyFields(p).setPickTimeSecFromOrigin(-100f)).collect(Collectors.toList()));
        secondWaveform = waveformService.save(secondWaveform);

        assertThat(secondWaveform.getAssociatedPicks()).size().isEqualTo(1).describedAs("Should still only have one pick on the waveform itself");
        assertThat(secondWaveform.getAssociatedPicks()).anyMatch(pick -> pick.getPickTimeSecFromOrigin() == -100f).describedAs("Pick should be a -100 offset now");
    }

    @Test
    public void testFindByUniqueFields() throws Exception {
        Waveform initialWaveform = genWaveform();
        Waveform firstWaveform = waveformService.save(new Waveform().mergeNonNullOrEmptyFields(initialWaveform));
        assertNotNull(
                waveformRepository.findByUniqueFields(
                        firstWaveform.getEvent().getEventId(),
                            firstWaveform.getStream().getStation().getNetworkName(),
                            firstWaveform.getStream().getStation().getStationName(),
                            firstWaveform.getLowFrequency(),
                            firstWaveform.getHighFrequency()),
                    "Should be able to find the station by the unique compound key Evid-Net-Sta-Freq");
    }

    @Test
    public void testSaveSameStationBandDifferentEvent() throws Exception {
        Waveform initialWaveform = genWaveform();
        waveformService.save(new Waveform().mergeNonNullOrEmptyFields(initialWaveform));
        waveformService.save(new Waveform().mergeNonNullOrEmptyFields(initialWaveform).setEvent(initialWaveform.getEvent().setEventId("111")));
        assertThat(waveformService.findAll()).size().isEqualTo(2).describedAs("Should have saved two waveform entries");
    }

    private Waveform genWaveform() {
        Waveform w = new Waveform(null,
                                  null,
                                  new Event().setEventId("123").setLatitude(0).setLongitude(0).setOriginTime(zeroDate),
                                  new Stream().setStation(new Station().setLatitude(0).setLongitude(0).setNetworkName("XX").setStationName("STA1")),
                                  zeroDate,
                                  zeroDate,
                                  "vel",
                                  "nm/s",
                                  1.0,
                                  1.5,
                                  4d,
                                  Boolean.TRUE);
        w.getAssociatedPicks().add(new WaveformPick().setPickName("f").setPickTimeSecFromOrigin(0f).setPickType("f").setWaveform(w));
        return w;
    }
}