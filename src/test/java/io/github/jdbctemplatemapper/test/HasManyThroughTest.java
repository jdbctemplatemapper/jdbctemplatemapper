package io.github.jdbctemplatemapper.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
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
          
          Skill s1 = new Skill("java");
          jtm.insert(s1);
          
          Skill s2 = new Skill("spring");
          jtm.insert(s2);
          
          Skill s3 = new Skill("aws");
          jtm.insert(s3);
          
          
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
          

        flag = true;
      }
    }
   

    //@Test
    public void hasManyThrough_test() {
        Query.type(Employee.class)
        .hasMany(Skill.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("employee_id", "skill_id")
        .populateProperty("skills")
        .execute(jtm);
    }
    
    @Test
    public void hasManyThrough2_test() {
        Query.type(Skill.class)
        .hasMany(Employee.class)
        .throughJoinTable("employee_skill")
        .throughJoinColumns("skill_id", "employee_id")
        .populateProperty("employees")
        .execute(jtm);
    }
    
}
