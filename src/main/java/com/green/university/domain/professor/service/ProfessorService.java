package com.green.university.domain.professor.service;

import com.green.university.domain.grade.entity.Grade;
import com.green.university.domain.grade.repository.GradeRepository;
import com.green.university.domain.professor.dto.ProfessorDto;
import com.green.university.domain.professor.dto.ReadSyllabusDto;
import com.green.university.domain.professor.dto.SyllaBusFormDto;
import com.green.university.domain.professor.entity.Professor;
import com.green.university.domain.professor.entity.Syllabus;
import com.green.university.domain.professor.repository.ProfessorRepository;
import com.green.university.domain.professor.repository.SyllaBusRepository;
import com.green.university.domain.professor.specification.ProfessorSpecification;
import com.green.university.domain.student.dto.StudentInfoForProfessorDto;
import com.green.university.domain.subject.dto.PenaltyResultFormDto;
import com.green.university.domain.subject.dto.SubjectForProfessorDto;
import com.green.university.domain.subject.dto.SubjectPeriodForProfessorDto;
import com.green.university.domain.subject.dto.UpdateStudentGradeFormDto;
import com.green.university.domain.subject.entity.StuSub;
import com.green.university.domain.subject.entity.StuSubDetail;
import com.green.university.domain.subject.entity.Subject;
import com.green.university.domain.subject.repository.StuSubDetailRepository;
import com.green.university.domain.subject.repository.StuSubRepository;
import com.green.university.domain.subject.repository.SubjectAiJobRepository;
import com.green.university.domain.subject.repository.SubjectRepository;
import com.green.university.domain.subject.service.StuSubService;
import com.green.university.global.exception.CustomRestfullException;
import com.green.university.global.utils.PenaltyCalculator;
import com.green.university.infra.ai.service.AiBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final SubjectRepository subjectRepository;
    private final StuSubRepository stuSubRepository;
    private final StuSubDetailRepository stuSubDetailRepository;
    private final SyllaBusRepository syllaBusRepository;
    private final ProfessorRepository professorRepository;
    private final StuSubService stuSubService;
    private final GradeRepository gradeRepository;
    private final AiBatchService aiBatchService;
    private static final int PAGE_SIZE = 20; // 교수 리스트 / 검색 페이징 용

    /**
     * 교수가 맡은 과목들의 학기 검색
     *
     * @param professorId
     * @return SubjectPeriodForProfessorDto list
     */
    @Transactional
    public List<SubjectPeriodForProfessorDto> selectSemester(Long professorId) {
        List<Subject> subjectList = subjectRepository.findByProfessor_Id(professorId);
        // 중복 subYear, semester 있을 수 있으니 distinct 처리하려면 Set 같은 별도 로직 필요할 수 있음
        // 여기선 리스트 전체를 dto로 변환해 반환하는 예제임
        return subjectList.stream()
                .map(subject -> {
                    SubjectPeriodForProfessorDto dto = new SubjectPeriodForProfessorDto();
                    dto.setId(professorId);
                    dto.setSubYear(subject.getSubYear());
                    dto.setSemester(subject.getSemester());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 년도와 학기, 교수 id를 이용하여 해당 과목의 정보 불러오기
     *
     * @param subjectPeriodForProfessorDto
     * @return SubjectForProfessorDto list
     */
    @Transactional
    public List<SubjectForProfessorDto> selectSubjectBySemester(
            SubjectPeriodForProfessorDto subjectPeriodForProfessorDto) {
        List<Subject> list = subjectRepository.findByProfessor_IdAndSubYearAndSemester(subjectPeriodForProfessorDto.getId(), subjectPeriodForProfessorDto.getSubYear(), subjectPeriodForProfessorDto.getSemester());
        if(subjectPeriodForProfessorDto.getSemester() == null || subjectPeriodForProfessorDto.getSubYear() == null) {
            list = subjectRepository.findByProfessor_Id(subjectPeriodForProfessorDto.getId());
        }
        return list.stream()
                .map(subject -> {
                    SubjectForProfessorDto subjectDto = new SubjectForProfessorDto();
                    subjectDto.setId(subject.getId());
                    subjectDto.setName(subject.getName());
                    subjectDto.setSubDay(subject.getSubDay());
                    subjectDto.setStartTime(subject.getStartTime());
                    subjectDto.setEndTime(subject.getEndTime());
                    subjectDto.setRoomId(subject.getRoom().getId());
                    return subjectDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 해당 과목을 듣는 학생의 세부정보 리스트로 불러오기 (교수 확인용)
     *
     * @param subjectId
     * @return StudentInfoForProfessorDto list
     */
    @Transactional
    public List<StudentInfoForProfessorDto> selectBySubjectId(Long subjectId) {

        return stuSubDetailRepository.findBySubject_Id(subjectId)
                .stream()
                .map(StudentInfoForProfessorDto::fromEntity)  // 각 StuSub → DTO 변환
                .collect(Collectors.toList());
    }

	/**
	 * 과목 id로 과목 Entity 불러오기
	 * 
	 * @param id
	 * @return
	 */
	@Transactional
	public Subject selectSubjectById(Long id) {
		Subject subjectEntity = subjectRepository.findById(id).orElseThrow(
				() -> new CustomRestfullException("해당 과목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND)
		);

        return subjectEntity;
    }

    // 해당 과목에 학생의 출결 및 성적 기입
    @Transactional
    public void updateGrade(Long subjectId, Long studentId, UpdateStudentGradeFormDto dto) {



        // 1. StuSub 찾기
        StuSub stuSub = stuSubRepository
                .findByStudent_IdAndSubject_Id(studentId, subjectId)
                .orElseThrow(() -> new RuntimeException("학생 과목 정보 없음"));

        // 2. StuSubDetail 찾기
        StuSubDetail detail = stuSubDetailRepository.findByStuSub(stuSub)
                .orElseThrow(() -> new RuntimeException("출결 정보 없음"));

        if(detail.isFinalized()) {
            throw new CustomRestfullException("이미 확정된 성적은 수정할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 3. 해당 강의 듣는 학생 수 계산 (상대평가 계산 용 / 수강 인원이 10명 이하면 절대평가)
        int numOfStudent = subjectRepository.findNumOfStudentById(subjectId);

        // 4. 기본 성적 입력
        detail.setAbsent(dto.getAbsent()); // 결석
        detail.setLateness(dto.getLateness()); // 지각
        detail.setHomework(dto.getHomework()); // 과제점수
        detail.setMidExam(dto.getMidExam()); // 중간
        detail.setFinalExam(dto.getFinalExam()); // 기말


        PenaltyResultFormDto penaltyResult = PenaltyCalculator.calculate(dto.getAbsent(), dto.getLateness());
        long totalAbsent = penaltyResult.getTotalAbsent();
        double penalty = penaltyResult.getPenalty();

        // 5. 환산점수 계산
        double convertedmark =
                (dto.getHomework() * 0.2) + // 과제 점수 20%
                        (dto.getMidExam() * 0.4) + // 중간 점수 40%
                        (dto.getFinalExam() * 0.4); // 기말 점수 40%

        // 최종 환산점수 계산 첫째 자리까지 반올림 (환산점수 - 감점)
        double finalConvertedMark = Math.round((convertedmark - penalty) * 10) / 10.0;

        // 5. 계산된 환산점수
        if (finalConvertedMark < 0) finalConvertedMark = 0;
        detail.setConvertedMark(finalConvertedMark);

        // 6. 등급 계산 (선택된 등급이 있으면 환산점수 까지만 계산하고, 등급은 수정됨)
        String letterGrade = null;

            if(numOfStudent < 20) { // 수강생이 20명 미만이면 절대평가
                letterGrade = getAbsoluteGrade(finalConvertedMark);
            }
             /*
             *   결석 5회 이상
             *   중간고사, 기말고사 40점 미만
             *   환산점수 60점 미만이면 - F
             */
            if(totalAbsent >= 5 || dto.getMidExam() < 40 || dto.getFinalExam() < 40 || finalConvertedMark < 60) { // 결석 5번 이상이면 F
                letterGrade = "F";
            }

        if(dto.getGrade() != null && !dto.getGrade().equals(detail.getLetterGrade())) { // 선택된 등급이 있고, 변경되었을 때
            letterGrade = dto.getGrade();
            System.out.println("letterGrade = " + letterGrade);
        }

        // 7. 등급 엔티티 조회
        if(letterGrade != null) {
            Grade grade = gradeRepository.findByLetterGrade(letterGrade)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 학점 등급입니다"));
            stuSub.setLetterGrade(grade);
            detail.setLetterGrade(grade.getLetterGrade()); // 단순 출력용으로 저장함
        }

        // 8. 저장
        stuSubDetailRepository.save(detail);
        stuSubRepository.save(stuSub);
        // 9. 최종 성적 가지고 이수학점 계산 (절대평가 전용)
        if(numOfStudent < 20) {
            stuSubService.updateCreditsFromLetterGrade(studentId, subjectId);
        }
    }


    // 절대평가 기준 등급 산출
    private static String getAbsoluteGrade(double score) {
        if (score >= 95) return "A+";
        if (score >= 90) return "A0";
        if (score >= 85) return "B+";
        if (score >= 80) return "B0";
        if (score >= 75) return "C+";
        if (score >= 70) return "C0";
        if (score >= 65) return "D+";
        if (score >= 60) return "D0";
        return "F";
    }

    /**
     * ☎️ 2. 과목 성적 최종 확정 + Job 저장 (별도 트랜잭션)
     *  AI 분석 비동기 트리거 (따로 나누기)
     */
    @Transactional
    public void finalizeGrades(Long subjectId) {
        List<StuSubDetail> details = stuSubDetailRepository.findBySubject_Id(subjectId);
        if (details.isEmpty()) {
            throw new CustomRestfullException("해당 과목에 성적 정보가 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 2. 모두 finalized=true로 변경 (성적 확정)
        details.forEach(d -> d.setFinalized(true));
        stuSubDetailRepository.saveAll(details);
        // 여기까지가 “빠르게 끝나는” 트랜잭션

        // ☎️ 3. Job 저장 (별도 트랜잭션)
        aiBatchService.createJob(subjectId, details.size());
    }


    /**
     * 교수 강의계획서 조회 (수정 시에도 필요)
     *
     * @param subjectId
     * @return 강의계획서
     */
    @Transactional
    public ReadSyllabusDto readSyllabus(Long subjectId) {
        // Subject로 찾은 후 이걸로 SyllaBus도 찾고, Professor도 찾기
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(
                () -> new CustomRestfullException("과목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND)
        );
        Syllabus syllabus = syllaBusRepository.findBySubject_Id(subjectId).orElseThrow(
                () -> new CustomRestfullException("강의 계획서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        Professor professor = subject.getProfessor();
        if (professor == null) {
            throw new CustomRestfullException("교수 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        ReadSyllabusDto dto = new ReadSyllabusDto(subject, professor, syllabus); // Dto에 생성자 추가함
        return dto;
    }

    /**
     * 강의 계획서 업데이트
     *
     * @param syllaBusFormDto
     */
    @Transactional
    public void updateSyllabus(Long id, SyllaBusFormDto syllaBusFormDto) {

        Syllabus syllabus = syllaBusRepository.findBySubject_Id(id).orElseThrow(
                () -> new CustomRestfullException("강의 계획서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        syllabus.setOverview(syllaBusFormDto.getOverview());
        syllabus.setObjective(syllaBusFormDto.getObjective());
        syllabus.setTextbook(syllaBusFormDto.getTextbook());
        syllabus.setProgram(syllaBusFormDto.getProgram());

        syllaBusRepository.save(syllabus);
    }

    /**
     * @return 교수 리스트 조회 + 검색
     */

    @Transactional
    public Page<ProfessorDto> readProfessorList(Long professorId, String deptName, Pageable pageable) {

        Specification<Professor> spec = (
                root, query, cb) -> null; // 조건 없이 전체 조회
        if (professorId != null) {
            spec = spec.and(ProfessorSpecification.hasProfessorId(professorId));
        }
        if (deptName != null) {
            spec = spec.and(ProfessorSpecification.hasDepartmentName(deptName));
        }
        if (deptName != null && professorId != null) {
            spec = spec.and(ProfessorSpecification.hasProfessorIdAndDepartmentName(professorId, deptName));
        }

        Page<Professor> Professor = professorRepository.findAll(spec, pageable);
        return Professor.map(ProfessorDto::fromEntity); // dto로 반환해주기

    }

}
