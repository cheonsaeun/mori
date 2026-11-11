# 에뮬레이터 제어와 MORI 상태 반영

## 사용하기

1. Pleos Connect 2.1.0을 켜고 Android Studio에서 MORI의 debug 앱을 실행합니다.
2. MORI에서 실제 차량 보기를 선택합니다. 디자인 미리보기는 실제 조작을 반영하지 않는 예시 화면입니다.
3. 헤더가 차량 시뮬레이터 연결됨으로 바뀌면 OS 하단의 공조/열선/통풍 버튼을 조작합니다.
4. MORI가 약 1초 간격으로 SDK 상태를 다시 읽어 표시합니다.
5. 앱 상단 차량 테스트에서 시트 자세 슬라이더, 열선/통풍, 온도, 풍량, 전원도 조작할 수 있습니다.

개발용 제어 패널은 공식 에뮬레이터의 제어 API에 명령을 보냅니다. 화면 값을 직접 바꾸지 않습니다. 전송 성공과 실제 반영은 다르므로, 실제 결과는 대시보드와 SDK 응답에서 확인합니다.

## OS 버튼이 동작하지 않았던 원인

이 이미지의 차량 시뮬레이터는 Virtual Console의 브라우저 세션이 활성화되어야 DDS 신호 중계를 활성화합니다. 서버가 실행 중이어도 활성 세션이 없으면 공조 publisher/subscriber가 모두 비활성 상태였습니다. OS에서 요청을 보내도 실제 상태가 돌아오지 않았고 온도 SDK 조회는 PropertyNotAvailableException으로 실패했습니다.

개발용 EmulatorSession은 Pleos AVD에서 공식 localhost:8088 세션을 유지하고 HVAC base 모드를 활성화합니다. 따라서 브라우저를 계속 열어 두지 않아도 MORI를 실행한 동안 OS 제어를 테스트할 수 있습니다. 앱을 강제 종료하면 세션도 만료될 수 있습니다. MORI 없이 시험하려면 공식 Virtual Console을 열어 둡니다.

공식 콘솔 열기: adb forward tcp:8088 tcp:8088 실행 후 http://127.0.0.1:8088/ 접속.

## 데이터 출처와 한계

| 항목 | MORI가 읽는 경로 |
| --- | --- |
| 좌석 점유 | Vehicle SDK |
| 공조 온도, 풍량, 전원 | Vehicle SDK |
| A/C, Auto, Sync, 내기순환 | Vehicle SDK |
| 열선/통풍 | Vehicle SDK. signed seat-temperature 영역은 양수=열선, 음수=통풍으로 해석 |
| 등받이 자세 | 개발용 Pleos 시뮬레이터의 실시간 recline 위치. 화면에 자세(시뮬)로 표시 |

현재 이미지에서는 시뮬레이터가 등받이 위치 40/55를 보유해도 SDK의 등받이 조회는 0을 반환합니다. 이 부분을 실제 SDK 연결 성공으로 표현하지 않습니다. 개발용 앱은 공식 시뮬레이터의 위치 신호를 받아 사용하며 고정 더미값으로 대체하지 않습니다. 값은 0~100 위치값이며 물리적인 각도 단위가 아닙니다.

Rear ventilation 등 미지원 항목은 미지원으로 유지합니다. 모든 OS 메뉴 기능이 MORI 화면 항목으로 구현된 것은 아닙니다.

## 개발용 코드 분리

세션 유지, 로컬 HTTP 접근, 테스트 제어 UI, 시뮬레이터 자세 신호, 상태 진단 로그는 src/debug에 둡니다. src/release의 같은 인터페이스는 네트워크와 제어를 하지 않습니다. HTTP 허용은 debug의 127.0.0.1/localhost에만 한정합니다. 실제 차량용 앱에 이 개발용 인터페이스를 포함하지 않습니다.

이 로컬 인터페이스는 설치된 Pleos 2.1.0 콘솔 구현에 의존합니다. 에뮬레이터 버전이 바뀌면 다시 검증해야 합니다. 실제 차량 제어 SDK 권한과 별개이며, 제공받은 클라이언트 시크릿은 필요하지 않습니다.

공식 콘솔 안내: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator
