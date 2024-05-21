package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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

      Employee e1 = new Employee("jane", "doe");
      jtm.insert(e1);

      Employee e2 = new Employee("joe", "smith");
      jtm.insert(e2);

      Employee e3 = new Employee("mike", "jones");
      jtm.insert(e3);

      Employee e4 = new Employee("greg", "william");
      jtm.insert(e4);

      Employee e5 = new Employee("jill", "karen");
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
  public void query_hasManyThrough_invalidJoinTableBlank_test() {
    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Employee.class)
           .hasMany(Skill.class)
           .throughJoinTable("    ")
           .throughJoinColumns("employee_id", "skill_id")
           .populateProperty("skills")
           .execute(jtm);
    });
    assertTrue(
        exception.getMessage().contains("throughJoinTable() tableName cannot be null or blank"));
  }


  @Test
  public void query_hasManyThrough_invalidFirstJoinColumnWithPrefix_test() {
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
  public void query_hasManyThrough_invalidFirstJoinColumnBlank_test() {
    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Employee.class)
           .hasMany(Skill.class)
           .throughJoinTable("employee_skill")
           .throughJoinColumns("   ", "skill_id")
           .populateProperty("skills")
           .execute(jtm);
    });
    assertTrue(exception.getMessage()
                        .contains("throughJoinColumns() typeJoinColumn cannot be null or blank"));
  }

  @Test
  public void query_hasManyThrough_invalidSecondJoinColumnWithPrefix_test() {
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
  public void query_hasManyThrough_invalidSecondJoinColumnBlank_test() {
    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Employee.class)
           .hasMany(Skill.class)
           .throughJoinTable("employee_skill")
           .throughJoinColumns("employee_id", "")
           .populateProperty("skills")
           .execute(jtm);
    });
    assertTrue(
        exception.getMessage()
                 .contains("throughJoinColumns() relatedTypeJoinColumn cannot be null or blank"));
  }

  @Test
  public void query_hasManyThrough_success_test() {
    List<Employee> employees = Query.type(Employee.class)
                                    .hasMany(Skill.class)
                                    .throughJoinTable("employee_skill")
                                    .throughJoinColumns("employee_id", "SKILL_ID")
                                    .populateProperty("skills")
                                    .orderBy("employee.id")
                                    .execute(jtm);

    assertTrue(employees.size() == 5);
    assertTrue(employees.get(0).getSkills().size() == 1);
    assertTrue(employees.get(1).getSkills().size() == 2);
    assertTrue(employees.get(2).getSkills().size() == 3);
    assertTrue(employees.get(3).getSkills().size() == 0);
    assertTrue(employees.get(4).getSkills().size() == 0);
  }

  @Test
  public void query_hasManyThrough_tableAlias_success() {
    List<Employee> employees = Query.type(Employee.class, "e")
                                    .hasMany(Skill.class, "s")
                                    .throughJoinTable("employee_skill")
                                    .throughJoinColumns("employee_id", "SKILL_ID")
                                    .populateProperty("skills")
                                    .orderBy("e.id, s.id")
                                    .execute(jtm);

    assertTrue(employees.size() == 5);
    assertTrue(employees.get(0).getSkills().size() == 1);
    assertTrue(employees.get(1).getSkills().size() == 2);
    assertTrue(employees.get(2).getSkills().size() == 3);
    assertTrue(employees.get(3).getSkills().size() == 0);
    assertTrue(employees.get(4).getSkills().size() == 0);
  }

  @Test
  public void query_hasManyThrough2_success_test() {
    List<Skill> skills = Query.type(Skill.class)
                              .hasMany(Employee.class)
                              .throughJoinTable("employee_skill")
                              .throughJoinColumns("skill_id", "employee_id")
                              .populateProperty("employees")
                              .orderBy("skill.id")
                              .execute(jtm);

    assertTrue(skills.size() == 4);
    assertTrue(skills.get(0).getEmployees().size() == 3);
    assertTrue(skills.get(1).getEmployees().size() == 2);
    assertTrue(skills.get(2).getEmployees().size() == 1);
    assertTrue(skills.get(3).getEmployees().size() == 0);
  }


  @Test
  public void queryMerge_hasManyThrough_invalidFirstJoinColumnBlank_test() {
    List<Employee> employees = Query.type(Employee.class).orderBy("employee.id").execute(jtm);

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      QueryMerge.type(Employee.class)
                .hasMany(Skill.class)
                .throughJoinTable("employee_skill")
                .throughJoinColumns("  ", "skill_id")
                .populateProperty("skills")
                .execute(jtm, employees);
    });
    assertTrue(exception.getMessage()
                        .contains("throughJoinColumns() typeJoinColumn cannot be null or blank"));
  }

  @Test
  public void queryMerge_hasManyThrough_success_test() {
    List<Employee> employees = Query.type(Employee.class).orderBy("employee.id").execute(jtm);

    QueryMerge.type(Employee.class)
              .hasMany(Skill.class)
              .throughJoinTable("employee_skill")
              .throughJoinColumns("employee_id", "skill_id")
              .populateProperty("skills")
              .orderBy("name")
              .execute(jtm, employees);

    assertTrue(employees.size() == 5);
    assertTrue(employees.get(0).getSkills().size() == 1);
    assertTrue(employees.get(1).getSkills().size() == 2);
    assertTrue(employees.get(2).getSkills().size() == 3);
    assertTrue(employees.get(3).getSkills().size() == 0);
    assertTrue(employees.get(4).getSkills().size() == 0);

    assertTrue("java".equals(employees.get(0).getSkills().get(0).getName()));
    assertTrue("aws".equals(employees.get(2).getSkills().get(0).getName()));

  }

  @Test
  public void queryMerge_hasManyThroughWithJoinTablePrefix_success() {
    List<Employee> employees = Query.type(Employee.class).orderBy("employee.id").execute(jtm);

    QueryMerge.type(Employee.class)
              .hasMany(Skill.class)
              .throughJoinTable("schema1.employee_skill")
              .throughJoinColumns("employee_id", "skill_id")
              .populateProperty("skills")
              .orderBy("name")
              .execute(jtm, employees);

    assertTrue(employees.size() == 5);
    assertTrue(employees.get(0).getSkills().size() == 1);
    assertTrue(employees.get(1).getSkills().size() == 2);
    assertTrue(employees.get(2).getSkills().size() == 3);
    assertTrue(employees.get(3).getSkills().size() == 0);
    assertTrue(employees.get(4).getSkills().size() == 0);

    assertTrue("java".equals(employees.get(0).getSkills().get(0).getName()));
    assertTrue("aws".equals(employees.get(2).getSkills().get(0).getName()));

  }

  @Test
  public void queryMerge_hasManyThroughWithJoinTablePrefixInvalid() {
    List<Employee> employees = Query.type(Employee.class).orderBy("employee.id").execute(jtm);

    Assertions.assertThrows(org.springframework.jdbc.BadSqlGrammarException.class, () -> {
      QueryMerge.type(Employee.class)
                .hasMany(Skill.class)
                .throughJoinTable("aaa.employee_skill")
                .throughJoinColumns("employee_id", "skill_id")
                .populateProperty("skills")
                .orderBy("name")
                .execute(jtm, employees);
    });

  }

  @Test
  public void queryMerge_hasManyThrough_tableAlias_success() {
    List<Employee> employees = Query.type(Employee.class).orderBy("employee.id").execute(jtm);

    QueryMerge.type(Employee.class)
              .hasMany(Skill.class, "s")
              .throughJoinTable("employee_skill")
              .throughJoinColumns("employee_id", "skill_id")
              .populateProperty("skills")
              .orderBy("s.name")
              .execute(jtm, employees);

    assertTrue(employees.size() == 5);
    assertTrue(employees.get(0).getSkills().size() == 1);
    assertTrue(employees.get(1).getSkills().size() == 2);
    assertTrue(employees.get(2).getSkills().size() == 3);
    assertTrue(employees.get(3).getSkills().size() == 0);
    assertTrue(employees.get(4).getSkills().size() == 0);

    assertTrue("java".equals(employees.get(0).getSkills().get(0).getName()));
    assertTrue("aws".equals(employees.get(2).getSkills().get(0).getName()));

  }

}
