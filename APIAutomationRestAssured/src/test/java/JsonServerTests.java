import Entities.Booking;
import Entities.BookingDates;
import Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class JsonServerTests {
        public static Faker faker;
        private static RequestSpecification request;
        private static Booking booking;
        private static BookingDates bookingDates;
        private static User user;
        private static String getToken;
        private static String getLogin;
        private static String getIdBooking;

        @BeforeAll
        public static void Setup(){
            RestAssured.baseURI = "http://localhost:3000";

            faker = new Faker();
            user = new User(faker.name().username(),
                    faker.name().firstName(),
                    faker.name().lastName(),
                    faker.internet().safeEmailAddress(),
                    faker.internet().password(8,10),
                    faker.phoneNumber().toString());

            //String chekin = faker.date.recent(10);
            //String checkout = faker.date.soon(10);

            //bookingDates = new BookingDates(chekin, checkout);
            bookingDates = new BookingDates("2018-01-02", "2018-01-03");

            booking = new Booking(user.getFirstName(), user.getLastName(),
                    (float)faker.number().randomDouble(2, 50, 100000),
                    true,bookingDates,
                    "");
            RestAssured.filters(new RequestLoggingFilter(),new ResponseLoggingFilter(), new ErrorLoggingFilter());
        }

        @BeforeEach
        void setRequest(){
            request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                    .contentType(ContentType.JSON)
                    .auth().basic("admin", "password123");
        }

        @Order(1)
        @Test
        public void authToken(){
            String payload =
                    "{\n" +
                            "   \"email\" : \"carolllinnaa@gmail.com\",   \n  " +
                            "   \"password\" : \"myrela1234\"  \n  " +
                            "}";
            Response response = request
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/register")
                    .then()
                    .extract().response();
            getToken = response.jsonPath().getString("accessToken");
            System.out.println("Token de Registro " + getToken);
        }

        @Order(2)
        @Test
        public void loginToken(){
            String payload =
                    "{\n" +
                            "   \"email\" : \"carolllinnaa@gmail.com\",   \n  " +
                            "   \"password\" : \"myrela1234\"  \n  " +
                            "}";
            Response response = request
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/login")
                    .then()
                    .extract().response();
            getLogin = response.jsonPath().getString("accessToken");
            System.out.println("Token de Login " + getLogin);
        }

        @Order(3)
        @Test
        public void getAllUsers_ReturnOk(){
            Response response = request
                    .when()
                    .get("/users")
                    .then()
                    .extract().response();

            Assertions.assertNotNull(response);
            Assertions.assertEquals(200, response.statusCode());
        }

        @Order(4)
        @Test
        public void  getUserByEmail_BookingExists_ReturnOk(){
            request
                    .when()
                    .queryParam("email", "myrela@mail.com")
                    .get("/users")
                    .then()
                    .assertThat().statusCode(200).and().time(lessThan(2000L))
                    .and()
                    .body("results", hasSize(greaterThan(0)));
        }

        @Order(5)
        @Test
        public void getAllBookings_ReturnOk(){
            Response response = request
                    .when()
                    .get("/bookings")
                    .then()
                    .extract().response();

            Assertions.assertNotNull(response);
            Assertions.assertEquals(200, response.statusCode());
        }

        @Order(6)
        @Test
        public void  getAllBookingsByUserFirstName_BookingExists_ReturnOk(){
            request
                    .when()
                    .queryParam("firstName", "Myrela")
                    .get("/bookings")
                    .then()
                    .assertThat().statusCode(200).and().time(lessThan(2000L))
                    .and()
                    .body("results", hasSize(greaterThan(0)));
        }

        @Order(7)
        @Test
        public void  createBooking_WithValidData_ReturnOk(){
            Booking test = booking;
            Response response = request
                    .contentType(ContentType.JSON)
                    .body(booking)
                    .header("Authorization", "Bearer " + getLogin)
                    .when()
                    .post("/bookings")
                    .then()
                    .assertThat().statusCode(201).and().time(lessThan(2000L))
                    .extract().response();
            getIdBooking = response.jsonPath().getString("id");
            System.out.println("Criado Booking - ID: " + getIdBooking);
        }

        @Order(8)
        @Test
        public void updateBooking_UserExists_ReturnOk(){
            String payload =
                    "{\n" +
                            "   \"firstname\" : \"Myrela\",   \n  " +
                            "   \"lastname\" : \"Caroline\",  \n  " +
                            "   \"totalprice\" : 250,  \n  " +
                            "   \"depositpaid\" : true,  \n  " +
                            "   \"bookingdates\" : " +
                            "{\n" +
                            "   \"checkin\" : \"2022-12-20\",   \n  " +
                            "   \"checkout\" : \"2022-12-25\"  \n  " +
                            "},\n" +
                            "   \"additionalneeds\" : \"Breakfast\"  \n " +
                            "}";

            request
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .header("Authorization", "Bearer " + getLogin)
                    .when()
                    .put("/bookings/" + getIdBooking)
                    .then()
                    .assertThat().statusCode(200).and().time(lessThan(2000L));
            System.out.println("Atualizado Booking - ID: " + getIdBooking);
        }

        @Order(9)
        @Test
        public void partialUpdateBooking_UserExists_ReturnOk(){
            String payload =
                    "{\n" +
                            "   \"firstname\" : \"Tânia\",   \n  " +
                            "   \"lastname\" : \"Azevedo\"  \n  " +
                            "}";

            request
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .header("Authorization", "Bearer " + getLogin)
                    .when()
                    .patch("/bookings/" + getIdBooking)
                    .then()
                    .assertThat().statusCode(200).and().time(lessThan(2000L));
            System.out.println("Atualizado Parcialmente Booking - ID: " + getIdBooking);
        }

        @Order(10)
        @Test
        public void deleteBooking_UserExists_ReturnOk(){
            request
                    .header("Authorization", "Bearer " + getLogin)
                    .when()
                    .delete("/bookings/" + getIdBooking)
                    .then()
                    .assertThat().statusCode(200).and().time(lessThan(2000L));
            System.out.println("Deletado Booking - ID: " + getIdBooking);
        }

        @Order(11)
        @Test
        public void ping_ReturnOk(){
            Response response = request
                    .when()
                    .get("/ping")
                    .then()
                    .extract().response();

            Assertions.assertNotNull(response);
            Assertions.assertEquals(200, response.statusCode());
        }
}


