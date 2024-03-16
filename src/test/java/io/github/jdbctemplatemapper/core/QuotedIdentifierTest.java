package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;
import io.github.jdbctemplatemapper.model.Quote;
import io.github.jdbctemplatemapper.model.QuoteDetail;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QuotedIdentifierTest {
  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void sqlFindById_test() throws Exception {
    Method method = getMethod(jtm.getClass(), "sqlFindById", Class.class);
    String sql = (String) ReflectionUtils.invokeMethod(method, jtm, Quote.class);
    assertTrue(sql.contains("\"schema1\".\"quote\""));
    assertTrue(sql.contains("\"Col 1\""));
  }
  
  @Test
  public void sqlForUpdate_test() throws Exception {
    TableMapping tableMapping = jtm.getTableMapping(Quote.class);
    Method method = getMethod(jtm.getClass(), "buildSqlAndParamsForUpdate", TableMapping.class);
    SqlAndParams sqlAndParams = (SqlAndParams) ReflectionUtils.invokeMethod(method, jtm, tableMapping);
    assertTrue(sqlAndParams.getSql().contains("\"schema1\".\"quote\""));
    assertTrue(sqlAndParams.getSql().contains("SET \"Col 1\""));
  }
  
  @Test
  public void query_quotedIdentifier_test() {
    Query.type(Quote.class).execute(jtm);
    assertTrue(true);
  }

  @Test
  public void findAll_quotedIdentifier_Test() {
    List<Quote> quotes = jtm.findAll(Quote.class, "col1");
    assertTrue(true);
  }

  @Test
  public void update_QuoteIdentifier_Test() throws Exception {
    Quote quote = jtm.findById(Quote.class, 1);
    jtm.update(quote);
    assertTrue(true);
  }
  
  
  @Test
  public void hasMany_Test() throws Exception {
  List<Quote> quotes = Query.type(Quote.class)
  .hasMany(QuoteDetail.class)
  .joinColumnManySide("quote_id")
  .populateProperty("details")
  .execute(jtm);
  
  assertTrue(quotes.size() == 1);
  assertTrue(quotes.get(0).getDetails().size() == 2);
  
  }
  
  @Test
  public void hasOne_Test() throws Exception {
  List<QuoteDetail> quoteDetails = Query.type(QuoteDetail.class)
  .hasOne(Quote.class)
  .joinColumnTypeSide("quote_id")
  .populateProperty("quote")
  .orderBy("quote_detail.\"Col 1\"")
  .execute(jtm);
  
  assertTrue(quoteDetails.size() == 2);
  assertTrue(quoteDetails.get(0).getQuote() != null);
  
  }
  
  private Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
    Method method = ReflectionUtils.findMethod(clazz, methodName, paramTypes);
    method.setAccessible(true);
    return method;
  }
}
