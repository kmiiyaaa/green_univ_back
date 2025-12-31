package com.green.university.domain.admin.service;

import com.green.university.domain.admin.dto.*;
import com.green.university.domain.admin.entity.Staff;
import com.green.university.domain.admin.entity.User;
import com.green.university.domain.admin.repository.StaffRepository;
import com.green.university.domain.admin.repository.UserRepository;
import com.green.university.domain.professor.dto.CreateProfessorFormDto;
import com.green.university.domain.professor.dto.ProfessorInfoDto;
import com.green.university.domain.professor.entity.Professor;
import com.green.university.domain.professor.repository.ProfessorRepository;
import com.green.university.domain.student.dto.*;
import com.green.university.domain.student.entity.Student;
import com.green.university.domain.student.repository.StuStatRepository;
import com.green.university.domain.student.repository.StudentRepository;
import com.green.university.domain.student.service.StuStatService;
import com.green.university.domain.university.repository.DepartmentRepository;
import com.green.university.global.exception.CustomRestfullException;
import com.green.university.global.security.JwtUtil;
import com.green.university.global.utils.TempPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 유저 서비스
 *
 * @author 김지현
 */
@Service
public class UserService {

    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StuStatService stuStatService;
    @Autowired
    private StuStatRepository stuStatRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private JwtUtil jwtUtil;

    // staff 생성 서비스로 먼저 staff_tb에 insert한 후 staff_tb에 생긴 id를 끌고와 user_tb에 생성함
    @Transactional
    public void createStaffToStaffAndUser(CreateStaffFormDto dto) {
        System.out.println(dto.getName());
        Staff staff = new Staff();
        staff.setName(dto.getName());
        staff.setGender(dto.getGender());
        staff.setAddress(dto.getAddress());
        staff.setTel(dto.getTel());
        staff.setEmail(dto.getEmail());
        staff.setBirthDate(dto.getBirthDate());
        staff.setHireDate(dto.getHireDate());
        Staff savedStaff = staffRepository.save(staff);
        Long staffId = savedStaff.getId();
        // User 엔티티 생성 및 저장 (공통 메서드 이용)
        createUser(staffId, "staff", dto.getName() );
    }

    // professor 생성 서비스로 먼저 professor_tb에 insert한 후 professor_tb에 생긴 id를 끌고와 user_tb에
    @Transactional
    public void createProfessorToProfessorAndUser(CreateProfessorFormDto dto) {
        Professor professor = new Professor();
        professor.setName(dto.getName());
        professor.setGender(dto.getGender());
        professor.setBirthDate(dto.getBirthDate());
        professor.setEmail(dto.getEmail());
        professor.setTel(dto.getTel());
        professor.setAddress(dto.getAddress());
        professor.setDepartment(departmentRepository.findById(dto.getDeptId())
                .orElseThrow(() -> new CustomRestfullException("없는 학과 정보입니다.", HttpStatus.NOT_FOUND)));
        // 필요한 필드 모두 dto에서 옮기기
        professor.setHireDate(dto.getHireDate());
        Professor savedProfessor = professorRepository.save(professor);
        Long professorId = savedProfessor.getId();

        // User 엔티티 생성 및 저장 (공통 메서드 이용)
        createUser(professorId, "professor", dto.getName());
    }

    // student 생성 서비스로 먼저 student_tb에 insert한 후 student_tb에 생긴 id를 끌고와 user_tb에
    @Transactional
    public void createStudentToStudentAndUser(CreateStudentFormDto dto) {
        Student student = new Student();
        student.setName(dto.getName());
        student.setBirthDate(dto.getBirthDate());
        student.setGender(dto.getGender());
        student.setAddress(dto.getAddress());
        student.setTel(dto.getTel());
        student.setEntranceDate(dto.getEntranceDate());
        student.setEmail(dto.getEmail());
        student.setGrade(dto.getGrade());
        student.setSemester(dto.getSemester());
        student.setDepartment(departmentRepository.findById(dto.getDeptId())
                .orElseThrow(() -> new CustomRestfullException("없는 학과 정보입니다.", HttpStatus.NOT_FOUND)));
        Student savedStudent = studentRepository.save(student);
        Long studentId = savedStudent.getId();

        stuStatService.createFirstStatus(studentId); // 학적 상태 생성 (재학)

        // User 엔티티 생성 및 저장 (공통 메서드 이용)
        createUser(studentId, "student", dto.getName());
    }

    // 로그인 -> JWT기반 변경
    @Transactional
    public LoginResponseDto login(LoginFormDto loginFormDto) {
        //유저 조회
        User user = userRepository.findById(Long.valueOf(loginFormDto.getId())).orElseThrow(
                () -> new CustomRestfullException("아이디를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)
        );

        //비밀번호 검증
        if (!passwordEncoder.matches(loginFormDto.getPassword(), user.getPassword())) {
            throw new CustomRestfullException("비밀번호가 틀렸습니다.", HttpStatus.BAD_REQUEST);
        }

        //JWT 액세스 토큰 발급
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUserRole());

        //응답 dto
        return new LoginResponseDto(
                user.getId(),
                user.getUserRole(),
                accessToken
        );
    }

    // 수정 할 학생 정보 불러오기
    public UserInfoForUpdateDto readStudentInfoForUpdate(Long userId) {
        Student student = studentRepository.findById(userId).orElseThrow(
                () -> new CustomRestfullException("학생 정보가 없습니다.", HttpStatus.NOT_FOUND)
        );
        UserInfoForUpdateDto userInfoForUpdateDto = new UserInfoForUpdateDto();
        userInfoForUpdateDto.setAddress(student.getAddress());
        userInfoForUpdateDto.setTel(student.getTel());
        userInfoForUpdateDto.setEmail(student.getEmail());
        return userInfoForUpdateDto;
    }

    // 수정 할 직원 정보 불러오기
    public UserInfoForUpdateDto readStaffInfoForUpdate(Long userId) {
        Staff staff = staffRepository.findById(userId).orElseThrow(
                () -> new CustomRestfullException("직원 정보가 없습니다.", HttpStatus.NOT_FOUND)
        );
        UserInfoForUpdateDto userInfoForUpdateDto = new UserInfoForUpdateDto();
        userInfoForUpdateDto.setAddress(staff.getAddress());
        userInfoForUpdateDto.setTel(staff.getTel());
        userInfoForUpdateDto.setEmail(staff.getEmail());
        return userInfoForUpdateDto;
    }

    // 수정 할 교수 정보 불러오기
    public UserInfoForUpdateDto readProfessorInfoForUpdate(Long userId) {
        Professor professor = professorRepository.findById(userId).orElseThrow(
                () -> new CustomRestfullException("교수 정보가 없습니다.", HttpStatus.NOT_FOUND)
        );
        UserInfoForUpdateDto userInfoForUpdateDto = new UserInfoForUpdateDto();
        userInfoForUpdateDto.setAddress(professor.getAddress());
        userInfoForUpdateDto.setTel(professor.getTel());
        userInfoForUpdateDto.setEmail(professor.getEmail());
        return userInfoForUpdateDto;
    }

    // 학생 정보 수정
    @Transactional
    public void updateStudent(UserUpdateFormDto updateDto) {
        Student student = studentRepository.findById(updateDto.getUserId()).orElseThrow(
                () -> new CustomRestfullException("학생 정보가 없습니다.", HttpStatus.NOT_FOUND)
        );
        student.setAddress(updateDto.getAddress());
        student.setTel(updateDto.getTel());
        student.setEmail(updateDto.getEmail());
        studentRepository.save(student);
    }

    // 직원 정보 수정
    @Transactional
    public void updateStaff(UserUpdateFormDto updateDto) {
        Staff staff = staffRepository.findById(updateDto.getUserId()).orElseThrow(
                () -> new CustomRestfullException("직원 정보가 없습니다.", HttpStatus.NOT_FOUND)
        );
        staff.setAddress(updateDto.getAddress());
        staff.setTel(updateDto.getTel());
        staff.setEmail(updateDto.getEmail());
        staffRepository.save(staff);
    }

    // 교수 정보 수정
    @Transactional
    public void updateProfessor(UserUpdateFormDto updateDto) {
        Professor professor = professorRepository.findById(updateDto.getUserId()).orElseThrow(
                () -> new CustomRestfullException("교수 정보가 없습니다.", HttpStatus.NOT_FOUND)
        );
        professor.setAddress(updateDto.getAddress());
        professor.setTel(updateDto.getTel());
        professor.setEmail(updateDto.getEmail());
        professorRepository.save(professor);
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(ChangePasswordFormDto changePasswordFormDto) {
        User user = userRepository.findById(changePasswordFormDto.getId()).orElseThrow(
                () -> new CustomRestfullException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)
        );
        user.setPassword(changePasswordFormDto.getAfterPassword());
        userRepository.save(user);
    }

    // 학생 조회
    @Transactional
    public StudentDto readStudent(Long studentId) {
        Student student = studentRepository.findById(studentId).orElseThrow(
                () -> new CustomRestfullException("학생을 조회할 수 없습니다.", HttpStatus.NOT_FOUND)
        );

        return StudentDto.fromEntity(student);

    }

    // 직원 조회
    @Transactional
    public Staff readStaff(Long id) {
        return staffRepository.findById(id).orElseThrow(
                () -> new CustomRestfullException("직원을 조회할 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    // 교수 조회
    @Transactional
    public ProfessorInfoDto readProfessorInfo(Long id) {
        Professor professor = professorRepository.findById(id).orElseThrow(
                () -> new CustomRestfullException("교수을 조회할 수 없습니다.", HttpStatus.NOT_FOUND));
        return new ProfessorInfoDto(professor); // dto 생성자 추가함
    }

    // 학생 정보 조회 (StudentInfoDto)
    @Transactional
    public StudentInfoDto readStudentInfo(Long id) {
        Student student = studentRepository.findById(id).orElseThrow(
                () -> new CustomRestfullException("학생 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND)
        );
        return StudentInfoDto.fromEntity(student);
    }

    // 아이디 찾기
    @Transactional(readOnly = true)
    public Long readIdByNameAndEmail(FindIdFormDto dto) {
        Long id = switch (dto.getUserRole()) {
            case "student" ->
                    studentRepository.findIdByNameAndEmail(dto.getName(), dto.getEmail());
            case "professor" ->
                    professorRepository.findIdByNameAndEmail(dto.getName(), dto.getEmail());
            case "staff" ->
                    staffRepository.findIdByNameAndEmail(dto.getName(), dto.getEmail());
            default ->
                    throw new CustomRestfullException("잘못된 userRole 입니다.", HttpStatus.BAD_REQUEST);
        };

        if (id == null) {
            throw new CustomRestfullException(
                    "조건에 맞는 정보를 찾을 수 없습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        return id;
    }


    @Transactional
    public String updateTempPassword(FindPasswordFormDto dto) {

        Long findId = switch (dto.getUserRole()) {
            case "student" ->
                    studentRepository.findByIdAndNameAndEmail(dto.getId(), dto.getName(), dto.getEmail());
            case "professor" ->
                    professorRepository.findByIdAndNameAndEmail(dto.getId(), dto.getName(), dto.getEmail());
            case "staff" ->
                    staffRepository.findByIdAndNameAndEmail(dto.getId(), dto.getName(), dto.getEmail());
            default ->
                    throw new CustomRestfullException("잘못된 userRole 입니다.", HttpStatus.BAD_REQUEST);
        };

        if (findId == null) {
            throw new CustomRestfullException(
                    "조건에 맞는 정보를 찾을 수 없습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String tempPassword = new TempPassword().returnTempPassword();

        User user = userRepository.findById(findId)
                .orElseThrow(() -> new CustomRestfullException(
                        "조건에 맞는 사용자를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        user.setPassword(passwordEncoder.encode(tempPassword));

        return tempPassword;
    }


    // studentId로 학생의 학적 변동 내역(StuStat)을 StudentInfoStatListDto로 가져오기
    public List<StudentInfoStatListDto> readStudentInfoStatListByStudentId(Long studentId) {
        List<StuStat> stuStatList = stuStatRepository.findByStudent_IdOrderByIdDesc(studentId);
        return stuStatList.stream()
                .map(StudentInfoStatListDto::fromEntity)
                .collect(Collectors.toList());
    }


    // =================================
    // 공통 메서드
    // =================================
    // id와 userRole 이용해서 User에 저장
    private void createUser(Long id, String userRole,String name) {
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(id + ""));
        user.setUserRole(userRole);
        user.setName(name);
        userRepository.save(user);
    }



}
