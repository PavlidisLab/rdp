package ubc.pavlab.rdp.security.authentication;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

public class TokenBasedAuthenticationConverterTest {

    private ServletContext servletContext = new MockServletContext();

    @Test
    public void convert_withAuthorizationHeader() {
        MockHttpServletRequest request = request( HttpMethod.GET, "/" )
                .header( "Authorization", "Bearer 1234" )
                .buildRequest( servletContext );
        assertThat( new TokenBasedAuthenticationConverter().convert( request ) )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "credentials", "1234" );
    }

    @Test
    public void convert_withUnsupportedAuthenticationScheme_thenIgnoreAndReturnNull() {
        MockHttpServletRequest request = request( HttpMethod.GET, "/" )
                .header( "Authorization", "Basic 1234" )
                .buildRequest( servletContext );
        assertThat( new TokenBasedAuthenticationConverter().convert( request ) )
                .isNull();
    }

    @Test
    public void convert_withAuthRequestParam() {
        MockHttpServletRequest request = request( HttpMethod.GET, "/" )
                .param( "auth", "1234" )
                .buildRequest( servletContext );
        assertThat( new TokenBasedAuthenticationConverter().convert( request ) )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "credentials", "1234" );
    }

    @Test
    public void convert_whenCredentialsAreMissing_thenReturnNull() {
        MockHttpServletRequest request = request( HttpMethod.GET, "/" )
                .buildRequest( servletContext );
        assertThat( new TokenBasedAuthenticationConverter().convert( request ) )
                .isNull();
    }
}