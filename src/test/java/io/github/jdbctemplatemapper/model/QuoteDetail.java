package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.QuotedIdentifiers;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "quote_detail")
@QuotedIdentifiers
public class QuoteDetail {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;
  
  @Column
  private Integer quoteId;
  
  private Quote quote;

  @Column(name = "Col 1")
  private String col1;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getQuoteId() {
    return quoteId;
  }

  public void setQuoteId(Integer quoteId) {
    this.quoteId = quoteId;
  }

  public String getCol1() {
    return col1;
  }

  public void setCol1(String col1) {
    this.col1 = col1;
  }

  public Quote getQuote() {
    return quote;
  }

  public void setQuote(Quote quote) {
    this.quote = quote;
  }

}

