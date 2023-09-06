package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "employee_skill")
public class EmployeeSkill {

    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;
    @Column
    private Integer employeeId;
    @Column
    private Integer skillId;
       
    public EmployeeSkill() {}
    
    public EmployeeSkill(Integer employeeId, Integer skillId) {
        this.employeeId = employeeId;
        this.skillId = skillId;
    }
        
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public Integer getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    public Integer getSkillId() {
        return skillId;
    }
    public void setSkillId(Integer skillId) {
        this.skillId = skillId;
    }   
}
