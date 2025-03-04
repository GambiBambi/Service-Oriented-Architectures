package org.bp.microbooking;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.camel.model.rest.RestParamType.body;
import static org.apache.camel.model.rest.RestParamType.path;
import static org.bp.microbooking.model.Visit.getDayOfWeek;
import static org.bp.microbooking.model.Visit.isSameDayAndValidTime;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.bp.microbooking.model.*;
import org.bp.microbooking.exceptions.AvailabilityException;
import org.bp.microbooking.exceptions.VisitException;
import org.bp.microbooking.exceptions.DiscountException;
import org.bp.microbooking.state.ProcessingEvent;
import org.bp.microbooking.state.ProcessingState;
import org.bp.microbooking.state.StateService;

import org.springframework.stereotype.Component;



@Component
public class VisitBookingService extends RouteBuilder{

    @org.springframework.beans.factory.annotation.Autowired
    BookingIdentifierService bookingIdentifierService;

    @org.springframework.beans.factory.annotation.Autowired
    BookingService bookingService;

    @org.springframework.beans.factory.annotation.Autowired
    StateService availabilityStateService;

    @org.springframework.beans.factory.annotation.Autowired
    StateService visitStateService;

    @org.springframework.beans.factory.annotation.Autowired
    StateService discountStateService;

    @org.springframework.beans.factory.annotation.Value("${microbooking.kafka.server}")
    private String microbookingKafkaServer;

    @org.springframework.beans.factory.annotation.Value("${microbooking.service.type}")
    private String microbookingServiceType;

    @Override
    public void configure() throws Exception {
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("availability"))
            availabilityExceptionHandlers();
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("visit"))
            visitExceptionHandlers();
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("discount"))
            discountExceptionHandlers();
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("gateway"))
            gateway();
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("availability"))
            availability();
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("visit"))
            visit();
        if (microbookingServiceType.equals("all") || microbookingServiceType.equals("discount"))
            discount();
//        availabilityExceptionHandlers();
//        visitExceptionHandlers();
//        discountExceptionHandlers();
//        gateway();
//        availability();
//        visit();
//        discount();
    }

    private void gateway() {
        restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .enableCORS(true)
                .contextPath("/api") //new
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Micro Visit booking API")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("cors", "true")
                .apiProperty("api.specification.contentType.json", "application/json")
                .apiProperty("api.specification.contentType.yaml", "application/yaml");

        rest("/visit").description("Micro Visit booking REST service")
                .consumes("application/json")
                .produces("application/json")
                .post("/booking").description("Book a visit").type(BookVisitRequest.class).outType(BookingInfo.class)
                    .param().name("body").type(body).description("The visit to book").endParam()
                    .responseMessage().code(200).message("Visit successfully booked").endResponseMessage()
                    .to("direct:bookVisit")
                .get("/result/{id}").description("Check if booking was successful")
                    .param().name("id").type(path).description("Booking Visit ID").dataType("string").endParam()
                    .responseMessage().code(200).message("Status retrieved").endResponseMessage()
                    .to("direct:checkIfSuccess");

        from("direct:checkIfSuccess").routeId("checkIfSuccess")
                .log("checkIfSuccess fired")
                .process(exchange -> {
                    log.info("Received ID: {}", exchange.getMessage().getHeader("id", String.class));
                    String id = exchange.getMessage().getHeader("id", String.class);
                    exchange.getMessage().setHeader("bookingVisitId", id);
                    boolean success = bookingService.success(id);
                    log.info("Success: {}", success);
                    if (success) {
                        String cost = bookingService.getBookingData(id).bookingInfo.getCost().toString();
                        exchange.getMessage().setBody(cost);
                    }
                    else {
                        exchange.getMessage().setBody(null);
                    }

                })
                .to("stream:out");


        from("direct:bookVisit").routeId("bookVisit")
                .log("bookVisit fired")
                .process((exchange) -> {
                    exchange.getMessage().setHeader("bookingVisitId",
                            bookingIdentifierService.getBookingIdentifier());
                })
                .to("direct:VisitBookRequest")
                .to("direct:bookRequester");

        from("direct:bookRequester").routeId("bookRequester")
                .log("bookRequester fired")
                .process(
                        (exchange) -> {
                            exchange.getMessage().setBody(Utils.prepareBookingInfo(
                                    exchange.getMessage().getHeader("bookingVisitId", String.class), null));
                        }
                );

        from("direct:VisitBookRequest").routeId("VisitBookRequest")
                .log("brokerTopic fired")
                .marshal().json()
                .to("kafka:VisitReqTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType);


        from("kafka:FinalInfoTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("finalBookingMsg")
                .log("finalBooking broker message fired")
                .unmarshal().json(JsonLibrary.Jackson, BookingInfo.class)
                .process(
                        (exchange) -> {
                            BookingInfo bi = exchange.getMessage().getBody(BookingInfo.class);
                            String bookingVisitId = bi.getId();
                            BookVisitRequest bvr = bi.getBvr();
                            bookingService.addBookVisitRequest(bookingVisitId, bvr);
                            bookingService.addAvailability(bookingVisitId, true);
                            bookingService.addBookingInfo(bookingVisitId, bi);
                            log.info("Booking visit: {}", bi.toString());
                            exchange.getMessage().setHeader("id", bookingVisitId);
                        }
                );

        from("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("failMsg")
                .log("failTopic broker message fired")
                .unmarshal().json(JsonLibrary.Jackson, ExceptionResponse.class)
                .process(
                        (exchange) -> {
                            ExceptionResponse er = exchange.getMessage().getBody(ExceptionResponse.class);
                            if (er != null ) {
                                log.info("Error: {}", er.toString());
                            }
                        }
                );

    }

    private void availability() {
        from("kafka:VisitReqTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("checkAvailability")
                .log("fired checkAvailability")
                .unmarshal().json(JsonLibrary.Jackson, BookVisitRequest.class)
                .process(
                        (exchange) -> {
                            String bookingVisitId =
                                    exchange.getMessage().getHeader("bookingVisitId", String.class);
                            ProcessingState previousState =
                                    availabilityStateService.sendEvent(bookingVisitId, ProcessingEvent.START);
                            if (previousState!=ProcessingState.CANCELLED) {
                                boolean ava = FALSE;
                                BookVisitRequest bvr = exchange.getMessage().getBody(BookVisitRequest.class);
//                                String id = exchange.getMessage().getHeader("bookingVisitId", String.class);
                                if (bvr!=null && bvr.getEmployee()!=null && bvr.getVisit()!=null
                                        && bvr.getEmployee().getFirstName()!=null
                                        && bvr.getEmployee().getLastName()!=null
                                        && bvr.getVisit().getDateStart()!=null
                                        && bvr.getVisit().getDateStop()!=null) {
                                    String firstName = bvr.getEmployee().getFirstName();
                                    String lastName = bvr.getEmployee().getLastName();
                                    Date dateStart = bvr.getVisit().getDateStart();
                                    Date dateStop = bvr.getVisit().getDateStop();
                                    String dayOfWeekStart = getDayOfWeek(dateStart);
                                    String dayOfWeekStop = getDayOfWeek(dateStop);
                                    if (firstName.equals("Julia") && lastName.equals("Podsadna")) {
                                        if(dayOfWeekStart.equals("MONDAY") || dayOfWeekStart.equals("TUESDAY") || dayOfWeekStart.equals("SATURDAY")) {
                                            ava = TRUE;
                                        }
                                        else { ava = FALSE; }
                                    }
                                    else if (firstName.equals("Julia") && lastName.equals("Kowalska")) {
                                        if(dayOfWeekStart.equals("WEDNESDAY") || dayOfWeekStart.equals("THURSDAY") || dayOfWeekStart.equals("FRIDAY") || dayOfWeekStart.equals("SUNDAY")) {
                                            ava = TRUE;
                                        }
                                        else { ava = FALSE; }
                                    }
                                    else {
                                        if(dayOfWeekStart.equals("SATURDAY") || dayOfWeekStart.equals("SUNDAY")) {
                                            ava = FALSE;
                                        }
                                        else { ava = TRUE; }
                                    }
                                    if(!ava) {
                                        throw new AvailabilityException("Employee not available: " + firstName + "; " + lastName + "; " + dateStart + " " + dateStop);
                                    }
                                }
                                bookingService.addBookVisitRequest(bookingVisitId, bvr);
                                bookingService.addAvailability(bookingVisitId, ava);
                                AvailabilityResponse response = new AvailabilityResponse();
                                response.setBookingId(bookingVisitId);
                                response.setAvailable(ava);
                                response.setRequest(bvr);
                                exchange.getMessage().setBody(response);
                                previousState = availabilityStateService.sendEvent(bookingVisitId,
                                        ProcessingEvent.FINISH);
                            }
                            exchange.getMessage().setHeader("previousState", previousState);
                        }
                )
                .marshal().json()
                .to("stream:out")
                .choice()
                    .when(header("previousState").isEqualTo(ProcessingState.CANCELLED))
                        .to("direct:availabilityCompensationAction")
                    .otherwise()
                        .setHeader("serviceType", constant("availability"))
                        .to("kafka:BookingTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType)
                .endChoice();

        from("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("availabilityCompensation")
                .log("fired availabilityCompensation")
                .unmarshal().json(JsonLibrary.Jackson, ExceptionResponse.class)
                .choice()
                    .when(header("serviceType").isNotEqualTo("availability"))
                        .process((exchange) -> {
                            String bookingVisitId = exchange.getMessage().getHeader("bookingVisitId", String.class);
                            ProcessingState previousState = availabilityStateService.sendEvent(bookingVisitId,
                                    ProcessingEvent.CANCEL);
                            exchange.getMessage().setHeader("previousState", previousState);
                        })
                    .choice()
                        .when(header("previousState").isEqualTo(ProcessingState.FINISHED))
                            .to("direct:availabilityCompensationAction")
                    .endChoice()
                .endChoice();

        from("direct:availabilityCompensationAction").routeId("availabilityCompensationAction")
                .log("fired availabilityCompensationAction")
                .process(
                        (exchange) -> {
                            String id = exchange.getMessage().getHeader("bookingVisitId", String.class);
                            bookingService.removeBooking(id);
                            ProcessingState previousState =
                                    availabilityStateService.sendEvent(id, ProcessingEvent.COMPLETE);
                            exchange.getMessage().setHeader("previousState", previousState);
                        }
                )
                .to("stream:out");
    }

    private void visit() {
        from("kafka:BookingTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("bookVisitService")
                .log("fired bookVisitService")
                .unmarshal().json(JsonLibrary.Jackson, AvailabilityResponse.class)
                .process(
                        (exchange) -> {
                            AvailabilityResponse response = exchange.getMessage().getBody(AvailabilityResponse.class);
                            String bookingVisitId = response.getBookingId();
                            BookVisitRequest bvr = response.getRequest();
                            ProcessingState previousState =
                                    visitStateService.sendEvent(bookingVisitId, ProcessingEvent.START);
                            if (previousState!=ProcessingState.CANCELLED) {
                                BookingInfo bi = new BookingInfo();
                                bi.setId(bookingVisitId);
                                if(response.isAvailable()) { //availability check
                                    if (bvr != null && bvr.getEmployee() != null && bvr.getVisit() != null
                                            && bvr.getEmployee().getFirstName() != null
                                            && bvr.getEmployee().getLastName() != null
                                            && bvr.getVisit().getDateStart()!=null
                                            && bvr.getVisit().getDateStop()!=null) {
                                        String firstName = bvr.getEmployee().getFirstName();
                                        String lastName = bvr.getEmployee().getLastName();
                                        Date dateStart = bvr.getVisit().getDateStart();
                                        Date dateStop = bvr.getVisit().getDateStop();
                                        Visit visit = bvr.getVisit();
                                        if (!isSameDayAndValidTime(dateStart, dateStop)) {
                                            throw new VisitException("Wrong date or time chosen: " + dateStart + "; " + dateStop);
                                        }
                                        if (firstName.equals("Julia") && lastName.equals("Podsadna")) {
                                            bi.setCost(new BigDecimal(500));
                                        } else if (firstName.equals("Julia") && lastName.equals("Kowalska")) {
                                            bi.setCost(new BigDecimal(400));
                                        } else {
                                            bi.setCost(new BigDecimal(300));
                                        }
                                    }
                                }
                                bi.setBvr(bvr);
                                bookingService.addBookingInfo(bi.getId(), bi);
                                exchange.getMessage().setBody(bi);
                                previousState = visitStateService.sendEvent(bookingVisitId,
                                        ProcessingEvent.FINISH);
                            }
                            exchange.getMessage().setHeader("previousState", previousState);
                        }
                )
                .marshal().json()
                .to("stream:out")
                .choice()
                    .when(header("previousState").isEqualTo(ProcessingState.CANCELLED))
                        .to("direct:bookVisitCompensationAction")
                    .otherwise()
                        .setHeader("serviceType", constant("visit"))
                        .to("kafka:BookingInfoTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType)
                .endChoice();

        from("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("bookVisitCompensation")
                        .log("fired bookVisitCompensation")
                        .unmarshal().json(JsonLibrary.Jackson, ExceptionResponse.class)
                        .choice()
                            .when(header("serviceType").isNotEqualTo("visit"))
                                .process((exchange) -> {
                                    String bookingVisitId = exchange.getMessage().getHeader("bookingVisitId",
                                            String.class);
                                    ProcessingState previousState = visitStateService.sendEvent(bookingVisitId,
                                            ProcessingEvent.CANCEL);
                                    exchange.getMessage().setHeader("previousState", previousState);
                                })
                            .choice()
                                .when(header("previousState").isEqualTo(ProcessingState.FINISHED))
                                    .to("direct:bookVisitCompensationAction")
                            .endChoice()
                        .endChoice();

        from("direct:bookVisitCompensationAction").routeId("bookVisitCompensationAction")
                .log("fired bookVisitCompensationAction")
                .process(
                        (exchange) -> {
                            String id = exchange.getMessage().getHeader("bookingVisitId", String.class);
                            bookingService.removeBooking(id);
                            ProcessingState previousState =
                                    visitStateService.sendEvent(id, ProcessingEvent.COMPLETE);
                            exchange.getMessage().setHeader("previousState", previousState);
                        }
                )
                .to("stream:out");
    }

    private void discount() {
        from("kafka:BookingInfoTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("discount")
                .log("fired discount")
                .unmarshal().json(JsonLibrary.Jackson, BookingInfo.class)
                .process(
                        (exchange) -> {
                            BookingInfo bi = exchange.getMessage().getBody(BookingInfo.class);
                            String bookingVisitId = bi.getId();
                            ProcessingState previousState =
                                    discountStateService.sendEvent(bookingVisitId, ProcessingEvent.START);
                            if (previousState!=ProcessingState.CANCELLED) {
                                BookVisitRequest bvr = bi.getBvr();
                                if (bvr != null && bvr.getCustomer() != null && bvr.getCard() != null
                                        && bvr.getCustomer().getFirstName() != null && bvr.getCustomer().getLastName() != null
                                        && bvr.getCard().getFirstName() != null && bvr.getCard().getLastName() != null
                                        && bvr.getCard().getNumber() != null) {
                                    String customerFirstName = bvr.getCustomer().getFirstName();
                                    String customerLastName = bvr.getCustomer().getLastName();
                                    String cardFirstName = bvr.getCard().getFirstName();
                                    String cardLastName = bvr.getCard().getLastName();
                                    String cardNumber = bvr.getCard().getNumber();
                                    BigDecimal cost = bi.getCost();
                                    BigDecimal discount = new BigDecimal(1);

                                    if (customerFirstName.equals(cardFirstName)
                                            && customerLastName.equals(cardLastName)
                                            && cardNumber.length() == 10) {
                                        discount = new BigDecimal("0.9");
                                    }
                                    else if (customerFirstName.equals(cardFirstName)
                                            && customerLastName.equals(cardLastName)
                                            && cardNumber.length() == 9) {
                                        discount = new BigDecimal("0.95");
                                    }
                                    else if (customerFirstName.equals(cardFirstName)
                                            && customerLastName.equals(cardLastName)) {
                                        discount = new BigDecimal("1");
                                    }
                                    else {
                                        throw new DiscountException("Wrong card data: " + cardNumber + "; " + cardFirstName + " " + cardLastName);
                                    }

                                    cost = cost.multiply(discount);
                                    bi.setCost(cost);
                                }

                                bookingService.addBookingInfo(bookingVisitId, bi);
                                exchange.getMessage().setBody(bi);
                                previousState = discountStateService.sendEvent(bookingVisitId,
                                        ProcessingEvent.FINISH);
                            }
                            exchange.getMessage().setHeader("previousState", previousState);
                        }
                )
                .marshal().json()
                .to("kafka:FinalInfoTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType);

        from("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType).routeId("discountCompensation")
                .log("fired discountCompensation")
                .unmarshal().json(JsonLibrary.Jackson, ExceptionResponse.class)
                .choice()
                    .when(header("serviceType").isNotEqualTo("discount"))
                        .process((exchange) -> {
                            String bookingVisitId = exchange.getMessage().getHeader("bookingVisitId", String.class);
                            ProcessingState previousState = discountStateService.sendEvent(bookingVisitId,
                                    ProcessingEvent.CANCEL);
                            exchange.getMessage().setHeader("previousState", previousState);
                        })
                    .choice()
                        .when(header("previousState").isEqualTo(ProcessingState.FINISHED))
                            .to("direct:discountCompensationAction")
                    .endChoice()
                .endChoice();

        from("direct:discountCompensationAction").routeId("discountCompensationAction")
                .log("fired discountCompensationAction")
                .process(
                        (exchange) -> {
                            String id = exchange.getMessage().getHeader("bookingVisitId", String.class);
                            bookingService.removeBooking(id);
                            ProcessingState previousState =
                                    visitStateService.sendEvent(id, ProcessingEvent.COMPLETE);
                            exchange.getMessage().setHeader("previousState", previousState);
                        }
                )
                .to("stream:out");
    }




    private void availabilityExceptionHandlers() {
        onException(AvailabilityException.class)
                .process((exchange) -> {
                    ExceptionResponse er = new ExceptionResponse();
                    er.setTimestamp(Date.from(OffsetDateTime.now().toInstant()));
                    Exception cause =
                            exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    er.setMessage(cause.getMessage());
                    exchange.getMessage().setBody(er);
                })
                .marshal().json()
                .to("stream:out")
                .setHeader("serviceType", constant("availability"))
                .to("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType)
                .handled(true);
    }

    private void visitExceptionHandlers() {
        onException(VisitException.class)
                .process((exchange) -> {
                    ExceptionResponse er = new ExceptionResponse();
                    er.setTimestamp(Date.from(OffsetDateTime.now().toInstant()));
                    Exception cause =
                            exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    er.setMessage(cause.getMessage());
                    exchange.getMessage().setBody(er);
                })
                .marshal().json()
                .to("stream:out")
                .setHeader("serviceType", constant("visit"))
                .to("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType)
                .handled(true);
    }

    private void discountExceptionHandlers() {
        onException(DiscountException.class)
                .process((exchange) -> {
                    ExceptionResponse er = new ExceptionResponse();
                    er.setTimestamp(Date.from(OffsetDateTime.now().toInstant()));
                    Exception cause =
                            exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    er.setMessage(cause.getMessage());
                    exchange.getMessage().setBody(er);
                })
                .marshal().json()
                .to("stream:out")
                .setHeader("serviceType", constant("discount"))
                .to("kafka:VisitBookingFailTopic?brokers=" + microbookingKafkaServer + "&groupId=" + microbookingServiceType)
                .handled(true);
    }

}
