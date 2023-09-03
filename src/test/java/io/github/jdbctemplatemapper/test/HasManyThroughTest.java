package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.exception.QueryException;
import io.github.jdbctemplatemapper.model.Employee;
import io.github.jdbctemplatemapper.model.EmployeeSkill;
import io.github.jdbctemplatemapper.model.Skill;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class HasManyThroughTest {
   
    @Autowired
    private JdbcTemplateMapper jtm;
    
    private static boolean flag = false;

    @BeforeEach
    public void setup() {
      if (!flag) {

          Employee  e1 = new Employee("jane", "doe");
          jtm.insert(e1);
         
          Employee  e2 = new Employee("joe", "smith");
          jtm.insert(e2);
          
          Employee  e3 = new Employee("mike", "jones");
           jtm.insert(e3);
           

           Employee  e4 = new Employee("greg", "william");
            jtm.insert(e4);
            
            Employee  e5 = new Employee("jill", "karen");
            jtm.insert(e5);
          
          Skill s1 = new Skill("java");
          jtm.insert(s1);
          
          Skill s2 = new Skill("spring");
          jtm.insert(s2);
          
          Skill s3 = new Skill("aws");
          jtm.insert(s3);
          
          Skill s4 = new Skill("oracle");
          jtm.insert(s4);
          
          
          EmployeeSkill e1s1 = new EmployeeSkill(e1.getId(), s1.getId());
          jtm.insert(e1s1);
          
          EmployeeSkill e2s1 = new EmployeeSkill(e2.getId(), s1.getId());
          jtm.insert(e2s1);
          
          EmployeeSkill e2s2 = new EmployeeSkill(e2.getId(), s2.getId());
          jtm.insert(e2s2);
          
          EmployeeSkill e3s1 = new EmployeeSkill(e3.getId(), s1.getId());
          jtm.insert(e3s1);
          
          EmployeeSkill e3s2 = new EmployeeSkill(e3.getId(), s2.getId());
          jtm.insert(e3s2);
          
          EmployeeSkill e3s3 = new EmployeeSkill(e3.getId(), s3.getId());
          jtm.insert(e3s3);
          
          EmployeeSkill e4s1 = new EmployeeSkill(null, s1.getId());
          jtm.insert(e4s1);
          
          EmployeeSkill e5s1 = new EmployeeSkill(e5.getId(), null);
          jtm.insert(e5s1);

         
        flag = true;
      }
    }
   
    @Test
    public void hasManyThrough_invalidJoinTableBlank_test() {
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("    ")
        .throughJoinColumns("employee_id", "skill_id")
        .populateProperty("skills")
        .execute(jtm);
        });
        assertTrue(exception.getMessage().contains("throughJoinTable cannot be blank"));
    }
    
    @Test
    public void hasManyThrough_invalidJoinTableWithPrefix_test() {
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("jdbctemplatemapper.employee_skill")
        .throughJoinColumns("employee_id", "skill_id")
        .populateProperty("skills")
        .execute(jtm);
        });
        assertTrue(exception.getMessage().contains("Invalid throughJoinTable"));
    }
    
    @Test
    public void hasManyThrough_invalidFirstJoinColumnWithPrefix_test() {
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("employee_skill.employee_id", "skill_id")
        .populateProperty("skills")
        .execute(jtm);
        });
        assertTrue(exception.getMessage().contains("Invalid throughJoinColumns"));
    }
    
    @Test
    public void hasManyThrough_invalidFirstJoinColumnBlank_test() {
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("   ", "skill_id")
        .populateProperty("skills")
        .execute(jtm);
        });
        assertTrue(exception.getMessage().contains("Invalid throughJoinColumns. Cannot be blank"));
    }
    
    @Test
    public void hasManyThrough_invalidSecondJoinColumnWithPrefix_test() {
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("employee_id", "employee_skill.skill_id")
        .populateProperty("skills")
        .execute(jtm);
        });
       assertTrue(exception.getMessage().contains("Invalid throughJoinColumns"));
    }
    
    @Test
    public void hasManyThrough_invalidSecondJoinColumnBlank_test() {
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("employee_id", "")
        .populateProperty("skills")
        .execute(jtm);
        });
        assertTrue(exception.getMessage().contains("Invalid throughJoinColumns. Cannot be blank"));
    }
    
    
    @Test
    public void hasManyThrough_sucess_test() {
        List<Employee> employees = Query.type(Employee.class)
        .orderBy("employee.id")
        .hasMany(Skill.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("employee_id", "skill_id")
        .populateProperty("skills")
        .execute(jtm);
                
        assertTrue(employees.size() == 5);
        assertTrue(employees.get(0).getSkills().size() == 1);
        assertTrue(employees.get(1).getSkills().size() == 2);
        assertTrue(employees.get(2).getSkills().size() == 3);
        assertTrue(employees.get(3).getSkills().size() == 0);
        assertTrue(employees.get(4).getSkills().size() == 0);      
    }
    
    @Test
    public void hasManyThrough2_success_test() {
        List<Skill> skills = Query.type(Skill.class)
                .orderBy("skill.id")
        .hasMany(Employee.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("skill_id", "employee_id")
        .populateProperty("employees")
        .execute(jtm);
        
        assertTrue(skills.size() == 4);
        assertTrue(skills.get(0).getEmployees().size() == 3);
        assertTrue(skills.get(1).getEmployees().size() == 2);
        assertTrue(skills.get(2).getEmployees().size() == 1);
        assertTrue(skills.get(3).getEmployees().size() == 0); 
    }
    
}
