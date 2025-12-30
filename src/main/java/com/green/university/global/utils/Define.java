package com.green.university.global.utils;

public class Define {

	public final static String PRINCIPAL = "principal";
	public final static String CREATE_FAIL = "생성에 실패하였습니다.";
	public final static String UPDATE_FAIL = "수정에 실패하였습니다.";
	public final static String NOT_FOUND_ID = "아이디를 찾을 수 없습니다.";
	public final static String WRONG_PASSWORD = "비밀번호가 틀렸습니다.";


	// 이미지 처리 관련
	// 1KB = 1024byte
	// 1MB = 1024*1024 = 1,048,476 byte
	//public final static String UPLOAD_DIRECTORY = "C:\\spring_upload\\universityManagement\\upload"; 로컬용 다운로드
    public final static String UPLOAD_DIRECTORY = "/home/ubuntu/uploads"; // 배포용 다운로드


	public final static Long MAX_FILE_SIZE = 1024L * 1024 * 20;

	/**
	 * 로그인 해야 접속 가능한 페이지 목록
	 *
     */
//	public final static String[] PATHS = { "/update", "/password", "/info/**", "/guide", "/notice/**"};
//	public final static String[] PROFESSOR_PATHS = { "/professor/**" };
//	public final static String[] STUDENT_PATHS = {"/grade/**"};
//	public final static String[] STAFF_PATHS = { "/user/**" };

//	public final static String[] PATHS = { "/api/update", "/api/password", "/api/info/**", "/api/guide", "/api/notice/**"};
//	public final static String[] PROFESSOR_PATHS = { "/api/professor/**", "/api/reserve/**", "/api/counseling/**",
//			"/api/counsel/**",	"/api/risk/**", "/api/evaluation/**",};
//	public final static String[] STUDENT_PATHS = {"/api/grade/**", "/api/grade/**", "/api/sugang/**","/api/subject/**",};
//	public final static String[] STAFF_PATHS = { "/api/admin/**", "/api/auth/**", "/api/sugangperiod/**", "/api/break/**",
//			"/api/notice/**", "/api/schedule/**", "/api/tuition/**","/api/user/**", "/api/personal/**" };


	// 수강 가능한 최대 학점
	public final static Long MAX_GRADES = 18L;

	// Subject 관련 페이지당 개수
	public final static int SUBJECT_PAGE_SIZE = 20;

	// Student 조회 페이지당 개수
	public final static int STUDENT_PAGE_SIZE = 20;

}
