# PHODO
:sparkles: Phodo 안드로이드 앱 개발 :sparkles:

## 음성인식 핸즈프리 캡쳐 기능

다음 단어 인식 : 치즈, 사진, 스마일, 김치, 캡처(캡쳐)

## 표정인식 핸즈프리 캡쳐 기능

happiness 지수 0.8 이상일 경우 자동 캡쳐

## MCU 파트

https://github.com/yyc9920/Capstone_RPi

## Face Customizing merge project(20/08/08)
<확인사항>
1. src 부분 => TF_face 폴더만 확인!(TF_face가 전체적인 face customizing 코드)

2. res/layout 부분 => 'tfe_od_~'로 시작하는 xml 파일과 image_edit_dialog xml 파일만 확인! 

3. 중요 클래스의 간단한 기능 설명.
    DetectorActivity.class + CameraActivity.class => MainActivity 역할 
    CameraConnectionFragment.class => Camera2 api 설정 코드
    LegacyCameraConnectionFragment.class => Camera1 api 설정 코드
    MultiBoxTracker.class => face detection 및 customizing 기능에 맞춰 tracking box 그려주는 코드

* 기존 코드는(face detection) 기능 구현 시 참고할 필요가 있어 유지함.

## CameraView lib with Face Detection(20/08/18)
Use CameraView library (link: https://github.com/natario1/CameraView)

< Support list in "FaceDetect(CameraView)" > 
1. take photo/video while face detecting.
2. handsfree capture(Optimal composition & Voice Recognition).
3. Auto Zoom toggle function when far from camera.

* FaceCustomizing part는 개발 중..

## Half Finalized CameraView lib with Face Detection(20/08/18)
Without Rail/Phodo Tracking Mode.

## Finalized Phodo, Face Detecting & Face Customizing (20/08/28)
Finalized Phodo version 1.
add control label & probability bluetooth data face customizing part. (20/09/03) 