package hello;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;
import static com.jayway.restassured.RestAssured.when;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.ValidatableResponse;

@RunWith(SpringJUnit4ClassRunner.class) 
@SpringApplicationConfiguration(classes = Application.class) 
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class OAuth2ControllerTest {
	
	private String authCode = null;
	private String token = null;

    @Value("${local.server.port}")
    int port;

    @Before
    public void setUp() {
    	RestAssured.port = port;
    }
    
    //normal public user
    private String webUser = "user";
    private String webUserPassword = "password";
    
    //App Server using this user/password to access api
    private String apiUser = "acme";
    private String apiUserPassword = "acme";
    
    @Test 
    public void canAuthorizeUserToOAuth2() {
    	RestAssured.authentication = preemptive().basic(webUser, webUserPassword);
    	
    	//when a normal user want to grant application right to act on behalf of him, will be redirected to /uaa/oauth/authorize
    	Map<String,String> cookies = 
    			
    	when()
	  		.get("/uaa/oauth/authorize?response_type=code&client_id=acme&redirect_uri=http://example.com")
	  	.then()
	  		.statusCode(HttpStatus.SC_OK)
	  	.extract()
	  		.cookies();
    	
    	//a normal user login and post to auth server to allow application to act on behalf of him
    	ValidatableResponse r = 
    	expect()
    	.given()
    		.cookies(cookies)
    		.formParam("user_oauth_approval", "true")
    		.formParam("scope.openid", "true")
    		.formParam("authorize", "Authorize")
    	.when()
  	  		.post("/uaa/oauth/authorize")
  	  	.then()
  	  		.header("Location", Matchers.startsWith("http://example.com?code="))
  	  		.statusCode(HttpStatus.SC_MOVED_TEMPORARILY);
    	String redirectUrL = r.extract().header("Location");
    	
    	Pattern p = Pattern.compile("code=(.*)");
    	Matcher m = p.matcher(redirectUrL);
    	m.find();
    	authCode = m.group(1);
    	
    	//normal user redirected to application with code, application use acme/acme and code to ask oauth server to grant a access token
    	ValidatableResponse r2 = 
    	given()
    		.auth().preemptive().basic(apiUser, apiUserPassword)
    		.formParam("code", authCode)
    		.formParam("client_id", apiUser)
    		.formParam("grant_type", "authorization_code")
    		.formParam("redirect_uri", "http://example.com")
    	.when()
    	    .post("/uaa/oauth/token")
  	  	.then()
  	  		.assertThat()
  	  		.contentType(ContentType.JSON)
  	  		.body("access_token", Matchers.notNullValue());
    	
    	token = r2.extract().body().jsonPath().getString("access_token");
    	
    	//application using the access token to get normal user's userinfo from resource servers
    	ValidatableResponse r3 = given()
    		.auth().oauth2(token)
		.when()
    		.get("/uaa/user")
    	.then()
    		.body("principal.username", Matchers.equalTo("user"));
    }

}
