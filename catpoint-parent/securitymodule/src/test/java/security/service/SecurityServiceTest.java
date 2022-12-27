// GET_PASSES_THIS_REPO_UDACITY_PLEASE

package security.service;

import image.service.FakeImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import security.data.SecurityRepository;
import security.model.AlarmStatus;
import security.model.ArmingStatus;
import security.model.Sensor;
import security.model.SensorType;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private BufferedImage bufferedImage;
    private SecurityService securityService;

    @Mock
    private FakeImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void init() {
        System.out.println("Init SecurityService");
        securityService = new SecurityService(securityRepository, imageService);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void whenAlarmArmedAndSensorActivated_pendingAlarm(ArmingStatus armingStatus) {

        Sensor sensor = new Sensor("Sensor 1", SensorType.DOOR);

        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void whenAlarmArmedAndSensorActivatedAndStatusPending_setAlarm(ArmingStatus armingStatus) {

        Sensor sensor = new Sensor("Sensor 1", SensorType.DOOR);

        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void whenPendingAlarmAndAllSensorsInactive_returnNoAlarm() {

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = new Sensor("Sensor 1", SensorType.DOOR);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void whenAlarmActive_changeSensorState_notEffectAlarm(boolean sensorActive) {

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        Sensor sensor = new Sensor("Sensor 1", SensorType.DOOR);
        sensor.setActive(sensorActive);

        securityService.changeSensorActivationStatus(sensor, !sensorActive);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void sensorActivatedAndStatusPending_changeAlarmState() {

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = new Sensor("Sensor 1", SensorType.DOOR, true);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    public void sensorDeactivated_alarmStateNoChange() {

        Sensor sensor = new Sensor("Sensor 1", SensorType.DOOR);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7. If the image service identifies an image containing a cat while the system is armed-home,
    // put the system into alarm status.
    @Test
    public void imageServiceIdentifiesCat_setAlarmStatus() {

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(bufferedImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the image service identifies an image that does not contain a cat,
    // change the status to no alarm as long as the sensors are not active.
    @Test
    public void imageServiceIdentifiesNoCat_changeStatusNoAlarm() {

        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(bufferedImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    public void systemDisarmed_noAlarm() {

        securityService.setArmingStatus((ArmingStatus.DISARMED));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void systemArmed_resetAllSensorsInactive(ArmingStatus armingStatus) {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor("Sensor 1", SensorType.DOOR, true));
        sensors.add(new Sensor("Sensor 2", SensorType.WINDOW, false));

        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }


    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void cameraShowsCat_changeAlarmStatusToAlarm() {

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}

