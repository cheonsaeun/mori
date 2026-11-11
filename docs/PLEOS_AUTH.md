# Pleos 발급 정보와 사용 범위

실제 발급 정보는 로컬 `secrets/pleos.env`에 보관합니다. 이 폴더는 Git 제외 대상이며 파일은 소유자만 읽고 쓸 수 있습니다. 저장소, APK, 문서, 로그에는 실제 값을 넣지 않습니다. 현재 차량 조회에는 이 파일을 읽는 코드가 없습니다.

공식 프로젝트 생성 문서에 따르면:

- 프로젝트 ID는 앱 심사 요청 시 App Market Console에 입력하는 정보입니다.
- 클라이언트 ID와 시크릿은 서버 연결을 요구하는 SDK 등에 사용됩니다.
- Pleos Connect Emulator 2.1.0 이후에는 CRN 입력이 요구되지 않습니다.
- Vehicle State와 NaviHelper는 신청 없이 사용할 수 있는 상태로 안내되어 있습니다.

현재 좌석/공조 조회에는 별도 인증 값을 삽입하지 않습니다. 서버 기반 SDK를 연결할 때 해당 SDK가 요구하는 인증 방식을 확인하고 연결합니다. 시크릿을 Android BuildConfig나 앱 리소스에 넣지 않습니다.

추가 서비스 발급이 필요하면 서비스명, 목적, 비용/결제 필요 여부, 필요한 설정을 먼저 안내합니다.

공식 문서: https://document.pleos.ai/docs/pleos-only/create-my-project
