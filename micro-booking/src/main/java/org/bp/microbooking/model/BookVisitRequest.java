/*
 * Micro service
 * Micro service to book a visit
 *
 * OpenAPI spec version: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package org.bp.microbooking.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * BookVisitRequest
 */

public class BookVisitRequest {
  @JsonProperty("customer")
  private Customer customer = null;

  @JsonProperty("visit")
  private Visit visit = null;

  @JsonProperty("employee")
  private Employee employee = null;

  @JsonProperty("card")
  private Card card = null;

  public BookVisitRequest card(Card card) {
    this.card = card;
    return this;
  }

  public Card getCard() {
    return card;
  }

  public void setCard(Card card) {
    this.card = card;
  }

  public BookVisitRequest customer(Customer customer) {
    this.customer = customer;
    return this;
  }

   /**
   * Get customer
   * @return customer
  **/
  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public BookVisitRequest visit(Visit visit) {
    this.visit = visit;
    return this;
  }

   /**
   * Get visit
   * @return visit
  **/
  public Visit getVisit() {
    return visit;
  }

  public void setVisit(Visit visit) {
    this.visit = visit;
  }

  public BookVisitRequest employee(Employee employee) {
    this.employee = employee;
    return this;
  }

   /**
   * Get employee
   * @return employee
  **/
  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookVisitRequest bookVisitRequest = (BookVisitRequest) o;
    return Objects.equals(this.customer, bookVisitRequest.customer) &&
        Objects.equals(this.visit, bookVisitRequest.visit) &&
        Objects.equals(this.employee, bookVisitRequest.employee) &&
        Objects.equals(this.card, bookVisitRequest.card);

  }

  @Override
  public int hashCode() {
    return Objects.hash(customer, visit, employee, card);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookVisitRequest {\n");
    
    sb.append("    customer: ").append(toIndentedString(customer)).append("\n");
    sb.append("    visit: ").append(toIndentedString(visit)).append("\n");
    sb.append("    employee: ").append(toIndentedString(employee)).append("\n");
    sb.append("    card: ").append(toIndentedString(card)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
