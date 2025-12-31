package com.green.university.domain.student.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * student_tb insert용
 * @author 김지현
 *
 */
@Data
public class CreateStudentFormDto {

    @NotEmpty(message = "이름을 입력해 주세요")
    @Size(min = 2, max = 30, message = "이름은 2자 이상 30자 이하로 입력해 주세요")
    private String name;

    @NotNull(message = "생년월일을 입력해 주세요")
    private LocalDate birthDate;

    @NotEmpty(message = "성별을 입력해 주세요")
    private String gender;

    @NotEmpty(message = "주소를 입력해 주세요")
    private String address;

    @NotBlank(message = "전화번호를 입력해 주세요")
    @Pattern(regexp = "^010-\\d{3,4}-\\d{4}$", message = "전화번호 양식(010-xxxx-xxxx)을 확인해주세요.")
    private String tel;

    @NotNull(message = "학과 번호를 입력해 주세요")
    @Positive(message = "학과 번호는는 숫자만 가능합니다.")
    private Long deptId;

    @NotNull(message = "입학 날짜를 입력해 주세요")
	private LocalDate entranceDate;

    @Email(message = "이메일 양식이 아닙니다.")
    @NotEmpty(message = "이메일을 입력해 주세요")
	private String email;

    @NotNull(message = "학년을 입력해 주세요")
    @Min(value = 1, message = "학년은 1~4만 가능합니다")
    @Max(value = 4, message = "학년은 1~4만 가능합니다")
    private Long grade;

    @NotNull(message = "학기를 입력해 주세요")
    @Min(value = 1, message = "학기는 1 또는 2만 가능합니다")
    @Max(value = 2, message = "학기는 1 또는 2만 가능합니다")
    private Long semester;
	
}
