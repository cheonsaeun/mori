# 초기 환경 검증

검증일: 2026-11-11. Apple Silicon Mac의 Pleos Connect 2.1.0 / Android API 34 에뮬레이터 기준입니다.

- Java 17, Gradle 8.10.2로 `:app:assembleDebug :app:lintDebug` 실행
- APK 설치와 MORI 시작 화면 실행
- 차량 조회 버튼에서 capability 확인 후 실제 도어 위치 조회 성공
- 운전석 왼쪽 문 위치 반환 값 0을 확인
- 개인 SDK 경로, Java 경로, .env, 인증 파일의 Git 제외 확인

Lint의 오류는 0건이며, 버전 고정에 따른 최신 버전 안내가 남을 수 있습니다. 이 프로젝트의 target API 34는 Pleos 실행 환경에 맞춘 초기 기준입니다.

현재 검증은 문 상태의 단건 조회입니다. 상태 변경 구독, 공조 쓰기, NaviHelper 길 안내, 음성 AI와 Gleo 연동은 아직 검증하지 않았습니다. GitHub Actions의 원격 실행 결과는 별도로 확인해야 합니다.
