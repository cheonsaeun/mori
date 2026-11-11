# 개발 시작하기

## 내 컴퓨터에서 열기

Android Studio의 Open에서 `/Users/saeuncheon/Desktop/26-2 CCP II/MORI/android`를 선택합니다. 저장소 최상위 MORI 폴더에는 서버와 문서도 있으므로, 앱을 열 때는 android 폴더를 선택합니다.

프로젝트를 신뢰할지 묻는 창이 나오면 자신이 복제한 MORI 경로인지 확인한 뒤 엽니다. 초기 Gradle 동기화에는 인터넷 연결이 필요합니다.

## Java 17 선택

Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK에서 Java 17을 선택합니다. 이 컴퓨터에는 temurin-17이 설치되어 있습니다. Android Studio에 포함된 Java가 25이면 이 프로젝트의 빌드에는 17을 선택합니다.

개인 Java 경로와 Android SDK 경로는 Git에 공유하지 않습니다. `local.properties`는 Android Studio가 컴퓨터에 맞춰 생성합니다.

## 차량 화면에서 실행

1. Tools → Device Manager에서 Pleos Connect v2.1.0을 실행합니다.
2. 차량 홈 화면이 완전히 나타날 때까지 기다립니다.
3. 상단 실행 구성을 app, 실행 기기를 Pleos로 선택합니다.
4. ▶ Run을 누릅니다.
5. 기본 화면에서 좌석과 공조 상태가 자동으로 갱신되는지 확인합니다.
6. 좌석을 누르면 선택 표시와 하단의 등받이 정보가 바뀝니다.
7. 상단 디자인 미리보기를 누르면 열선, 통풍, 탑승자, 음악의 예시 상태를 볼 수 있습니다. 실제 차량 보기를 누르면 실제 데이터로 돌아갑니다.
8. 스타일 가이드를 누르면 색상, 타이포그래피와 공통 컴포넌트를 확인합니다.

앱 화면이 활성화된 동안 조회 완료 후 약 1초 간격으로 상태를 다시 읽습니다. 현재 설치된 Pleos 이미지에서 CAR_SEATS와 CAR_CLIMATE는 일반 권한이며 앱 manifest에 선언되어 있습니다. 다른 이미지의 권한 조건은 별도 확인이 필요합니다.

Virtual Console에서 Seat Occupancy, Seat Temperature, 공조 상태 등을 바꾸면 앱의 다음 조회에서 반영되는지 확인할 수 있습니다. 지원 범위에 포함되지 않은 항목은 미지원, 조회에 실패한 값은 확인 불가로 표시합니다. debug 앱의 차량 테스트 패널에서는 에뮬레이터에 제어 명령을 보낼 수 있습니다. [제어 안내](EMULATOR_CONTROLS.md)를 참고합니다.

발급 정보의 용도와 보관 방식은 [Pleos 인증 정보](PLEOS_AUTH.md)에 정리합니다.

## 오류가 날 때

| 증상 | 확인할 내용 |
| --- | --- |
| Gradle Java 버전 오류 | Gradle JDK가 17인지 확인 |
| SDK 위치를 찾지 못함 | Android Studio SDK Manager에서 SDK 위치 확인 |
| 라이브러리 다운로드 실패 | 인터넷, Pleos Maven 접근과 버전 확인 |
| 실행할 기기가 없음 | Device Manager에서 Pleos 시작 |
| 차량 조회 실패 | 차량 서비스, 지원 영역, 조회 권한 확인 |
| 주행 중 화면이 닫힘/차단됨 | 주차 상태에서 실행. 플랫폼 제한을 우회하지 않음 |

빌드 APK는 `android/app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. APK와 개인 설정 파일은 Git에 올리지 않습니다.
