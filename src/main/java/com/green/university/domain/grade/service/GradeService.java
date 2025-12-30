package com.green.university.domain.grade.service;

import com.green.university.domain.evaluation.entity.Evaluation;
import com.green.university.domain.evaluation.repository.EvaluationRepository;
import com.green.university.domain.grade.dto.GradeDto;
import com.green.university.domain.grade.dto.GradeForScholarshipDto;
import com.green.university.domain.grade.dto.MyGradeDto;
import com.green.university.domain.grade.entity.Grade;
import com.green.university.domain.subject.entity.StuSub;
import com.green.university.domain.subject.entity.Subject;
import com.green.university.domain.subject.repository.StuSubRepository;
import com.green.university.global.utils.TermUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GradeService {

    @Autowired
    private StuSubRepository stuSubRepository;
    @Autowired
    private EvaluationRepository evaluationRepository;

    // 학생이 수강신청한 연도 조회
    public List<GradeDto> readGradeYearByStudentId(Long studentId) {

        List<StuSub> stuSubs =
                stuSubRepository.findByStudent_IdOrderBySubject_SubYearDescSubject_SemesterDesc(studentId);

        // 연도만 추출해 중복 제거
        List<Long> years = stuSubs.stream()
                .map(ss -> ss.getSubject().getSubYear())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<GradeDto> result = new ArrayList<>();
        for (Long year : years) {
            GradeDto dto = new GradeDto();
            dto.setSubYear(year);
            result.add(dto);
        }

        return result;
    }

    // 학생이 수강 신청한 학기 조회
    public List<GradeDto> readGradeSemesterByStudentId(Long studentId) {

        List<StuSub> stuSubs = stuSubRepository.findByStudent_IdOrderBySubject_SubYearDescSubject_SemesterDesc(studentId);

        // 연도, 학기 중복 제거
        Set<String> seen = new HashSet<>();
        List<GradeDto> result = new ArrayList<>();

        for (StuSub ss : stuSubs) {
            Subject subject = ss.getSubject();
            if (subject == null) continue;

            Long year = subject.getSubYear();
            Long semester = subject.getSemester();
            if (year == null || semester == null) continue;

            String key = year + "-" + semester;
            if (seen.add(key)) {
                GradeDto dto = new GradeDto();
                dto.setSubYear(year);
                dto.setSemester(semester);
                result.add(dto);
            }
        }
        return result;
    }

    // 금학기 성적 조회
    public List<GradeDto> readThisSemesterByStudentId(Long studentId) {

        Long currentYear = Long.valueOf(TermUtil.currentYear());
        Long currentSemester = Long.valueOf(TermUtil.currentSemester());

        List<StuSub> stuSubs = stuSubRepository.findByStudent_IdAndSubject_SubYearAndSubject_Semester(studentId, currentYear, currentSemester);

        return stuSubs.stream()
                .map(this::toGradeDto)
                .collect(Collectors.toList());

    }

    // 금학기 누계성적 조회
    public MyGradeDto readMyGradeByStudentId(Long studentId) {
        Long currentYear = TermUtil.currentYear();
        Long currentSemester = TermUtil.currentSemester();

        List<StuSub> stuSubs = stuSubRepository.findByStudent_IdAndSubject_SubYearAndSubject_Semester(studentId, currentYear, currentSemester);

        if(stuSubs.isEmpty()){
            return null;
        }

        return calculateMyGradeDto(studentId, currentYear,currentSemester, stuSubs);
    }

    // 전체 누계 성적 조회
    public List<MyGradeDto> readgradeinquiryList(Long studentId) {

        List<StuSub> stuSubs =
                stuSubRepository.findByStudent_IdOrderBySubject_SubYearDescSubject_SemesterDesc(studentId);

        if (stuSubs.isEmpty()) {
            return Collections.emptyList();
        }

        // 이번학기 강의평가 완료 여부 확인
        Long currentYear = TermUtil.currentYear();
        Long currentSemester = TermUtil.currentSemester();

        // 이번학기 수강과목만 추출
        List<StuSub> currentTermStuSubs = stuSubs.stream()
                .filter(ss -> ss.getSubject() != null)
                .filter(ss -> ss.getSubject().getSubYear() != null && ss.getSubject().getSemester() != null)
                .filter(ss -> Objects.equals(ss.getSubject().getSubYear(), currentYear)
                        && Objects.equals(ss.getSubject().getSemester(), currentSemester))
                .collect(Collectors.toList());

        // 강의평가가 모두 완료되지 않았으면, 이번학기 성적은 "표시에서만" 제외(접근은 허용)
        boolean hasAllEvalThisTerm = hasCompletedAllEvaluations(studentId, currentTermStuSubs);

        //연도, 학기별 그룹 TODO 그룹화 하는 이유?
        Map<String, List<StuSub>> grouped = stuSubs.stream()
                .filter(ss -> ss.getSubject() != null)
                .filter(ss -> ss.getSubject().getSubYear() != null && ss.getSubject().getSemester() != null)
                // ✅ 강의평가 미완료면 이번학기만 그룹에서 제외
                .filter(ss -> {
                    if (hasAllEvalThisTerm) return true;
                    return !(Objects.equals(ss.getSubject().getSubYear(), currentYear)
                            && Objects.equals(ss.getSubject().getSemester(), currentSemester));
                })
                .collect(Collectors.groupingBy(
                        ss -> ss.getSubject().getSubYear()+"-"+ss.getSubject().getSemester()
                ));

        List<MyGradeDto> result = new ArrayList<>();
        for (Map.Entry<String, List<StuSub>> entry : grouped.entrySet()) {

            String key = entry.getKey(); // 예: "2023-1"
            List<StuSub> list = entry.getValue();

            String[] parts = key.split("-");
            Long year = Long.valueOf(parts[0]);
            Long semester = Long.valueOf(parts[1]);

            result.add(calculateMyGradeDto(studentId, year, semester, list));
        }

        return result;

    }

    // 학기별 성적 조회 (전체 조회)
    public List<GradeDto> readAllGradeByStudentId(Long studentId) {

        List<StuSub> stuSubs = stuSubRepository.findByStudent_Id(studentId);

        return stuSubs.stream()
                .map(this::toGradeDto)
                .collect(Collectors.toList());
    }

    // 학기별 성적 조회 (선택 조회 - type 필터)
    public List<GradeDto> readGradeByType(Long studentId, Long subYear, Long semester, String type) {

        List<StuSub> stuSubs = stuSubRepository.findByStudent_IdAndSubject_SubYearAndSubject_SemesterAndSubject_Type(studentId, subYear, semester, type);

        return stuSubs.stream()
                .map(this::toGradeDto)
                .collect(Collectors.toList());

    }

    // 학기별  성적 조회 (type 무시)
    public List<GradeDto> readGradeByStudentId(Long studentId, Long subYear, Long semester) {

        List<StuSub> stuSubs =
                stuSubRepository.findByStudent_IdAndSubject_SubYearAndSubject_Semester(
                        studentId, subYear, semester
                );

        return stuSubs.stream()
                .map(this::toGradeDto)
                .collect(Collectors.toList());
    }

    // 평균 성적 조회
    public GradeForScholarshipDto readAvgGrade(Long studentId, Long subYear, Long semester) {

        // 특정 년도/학기에 수강한 모든 과목 가져오기
        List<StuSub> stuSubs = stuSubRepository.findByStudent_IdAndSubject_SubYearAndSubject_Semester(studentId, subYear, semester);

        if(stuSubs.isEmpty()) {
            return null;
        }

        double totalWeighted = 0.0; // 학점 * 등급값 합
        double totalGrades = 0.0; // 학점 합

        for(StuSub ss : stuSubs){
            Subject subject = ss.getSubject();
            Grade grade = ss.getLetterGrade();

            // subject 또는 grade가 null 이면 계산 스킵
            if(subject == null || grade == null) continue;

            Long subjectGrades = subject.getCredits();
            Double gradePoint = grade.getGradePoint();

            // 학점 또는 등급값이 null이면 스킵
            if(subjectGrades == null || gradePoint == null) continue;

            totalWeighted += subjectGrades * gradePoint;
            totalGrades += subjectGrades;

        }

        double avg = (totalGrades == 0 ) ? 0.0 : totalWeighted / totalGrades;

        GradeForScholarshipDto dto = new GradeForScholarshipDto();
        dto.setStudentId(studentId);
        dto.setSubYear(subYear);
        dto.setSemester(semester);
        dto.setAvgGrade(avg);

        return dto;

    }

    // 헬퍼 : StuSub -> GradeDto 변환
    private GradeDto toGradeDto(StuSub ss) {

        GradeDto dto = new GradeDto();

        Subject subject = ss.getSubject();
        Grade grade = ss.getLetterGrade();
        Evaluation evaluation = evaluationRepository
                .findByStudent_IdAndSubject_Id(
                        ss.getStudent().getId(),
                        ss.getSubject().getId()
                )
                .orElse(null);
        dto.setEvaluationId(
                evaluation != null ? evaluation.getId() : null
        );
        dto.setSubYear(subject.getSubYear());
        dto.setSemester(subject.getSemester());
        dto.setSubjectId(subject.getId());
        dto.setName(subject.getName());
        dto.setType(subject.getType());
        if (subject.getCredits() != null) {
            dto.setCredits(String.valueOf(subject.getCredits())); // 이수 학점
        }

        if (grade != null) {
            dto.setLetterGrade(grade.getLetterGrade()); // "A+", "B0"
            if (grade.getGradePoint() != null) {
                dto.setGradePoint(String.valueOf(grade.getGradePoint())); // "4", "3" 등
            }
        }
        return dto;
    }

    // ✅ 헬퍼 : 이번학기 수강 과목들에 대해 "모든 강의평가 완료" 여부 확인
    private boolean hasCompletedAllEvaluations(Long studentId, List<StuSub> currentTermStuSubs) {
        // 이번학기 수강이 없으면 제한할 필요 없음
        if (currentTermStuSubs == null || currentTermStuSubs.isEmpty()) {
            return true;
        }

        for (StuSub ss : currentTermStuSubs) {
            if (ss == null || ss.getSubject() == null || ss.getSubject().getId() == null) continue;

            boolean exists = evaluationRepository
                    .findByStudent_IdAndSubject_Id(studentId, ss.getSubject().getId())
                    .isPresent();

            if (!exists) {
                return false; // 하나라도 없으면 미완료
            }
        }

        return true;
    }

    // 헬퍼 : 누계 성적(MyGradeDto) 계산
    private MyGradeDto calculateMyGradeDto(Long studentId, Long year, Long semester, List<StuSub> stuSubs) {

        MyGradeDto dto = new MyGradeDto();
        dto.setStudentId(studentId);
        dto.setSubYear(year);
        dto.setSemester(semester);

        long sumGrades = 0;  // 이수해야 할 학점(시도 학점)
        long myCredits = 0;   // 실제 이수 학점

        double totalWeighted = 0.0;
        double totalGrades = 0.0;

        for (StuSub ss : stuSubs) {
            Subject subject = ss.getSubject();
            Grade grade = ss.getLetterGrade();
            if (subject == null) continue;

            Long subjectGrades = subject.getCredits(); // 과목 학점 수
            if (subjectGrades == null) subjectGrades = 0L;

            // 전체 시도 학점
            sumGrades += subjectGrades;

            // 성적이 있는 과목만 "이수"로 간주
            if (grade != null && ss.getCredits() != null) {
                myCredits +=  ss.getCredits();
                totalWeighted += subjectGrades * grade.getGradePoint();
                totalGrades += subjectGrades;
            }
        }

        dto.setTotalCredits(sumGrades);
        dto.setMyGrades(myCredits);

        float avg = (totalGrades == 0) ? 0.0f : (float) (totalWeighted / totalGrades);
        dto.setAverage(avg);

        return dto;
    }
}
