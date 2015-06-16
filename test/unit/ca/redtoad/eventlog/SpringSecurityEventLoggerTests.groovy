package ca.redtoad.eventlog

import grails.test.mixin.Mock
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.junit.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent

import javax.servlet.http.HttpServletRequest

@Mock([SpringSecurityEvent])
class SpringSecurityEventLoggerTests {

    def logger = new SpringSecurityEventLogger()

    @Test
    void testLogAuthenticationEventWithNullAuthentication() {
        logger.logAuthenticationEvent("event", null, "127.0.0.1", null)

        assert SpringSecurityEvent.count() == 1
        def event = SpringSecurityEvent.list().first()
        assert event.username == null
        assert event.sessionId == null
        assert event.eventName == "event"
        assert event.switchedUsername == null
        assert event.remoteAddress == "127.0.0.1"
    }

    @Test
    void testLogAuthenticationEventWithStringPrincipal() {
        def authentication = new TestingAuthenticationToken("username", [])
        logger.logAuthenticationEvent("event", authentication, "127.0.0.1", null)

        assert SpringSecurityEvent.count() == 1
        def event = SpringSecurityEvent.list().first()
        assert event.username == "username"
        assert event.sessionId == null
        assert event.eventName == "event"
        assert event.switchedUsername == null
        assert event.remoteAddress == "127.0.0.1"
    }

    @Test
    void testLogAuthenticationEventWithUserDetailsPrincipal() {
        def principal = { -> "username" } as UserDetails
        def authentication = new TestingAuthenticationToken(principal, [])
        logger.logAuthenticationEvent("event", authentication, "127.0.0.1", null)

        assert SpringSecurityEvent.count() == 1
        def event = SpringSecurityEvent.list().first()
        assert event.username == "username"
        assert event.sessionId == null
        assert event.eventName == "event"
        assert event.switchedUsername == null
        assert event.remoteAddress == "127.0.0.1"
    }

    @Test
    void testLogAuthenticationSwitchUserEvent() {
        def principal = { -> "username" } as UserDetails
        def authentication = new TestingAuthenticationToken(principal, [])
        authentication.details = [remoteAddress: '127.0.0.1', sessionId: 'mockSessionId']
        def targetUser = { -> "switchedUsername" } as UserDetails

        logger.onApplicationEvent(new AuthenticationSwitchUserEvent(authentication, targetUser))

        assert SpringSecurityEvent.count() == 1
        def event = SpringSecurityEvent.list().first()
        assert event.username == "username"
        assert event.sessionId == "mockSessionId"
        assert event.eventName == "AuthenticationSwitchUserEvent"
        assert event.switchedUsername == "switchedUsername"
        assert event.remoteAddress == "127.0.0.1"
    }

    @Test
    void testGetRemoteAddrFromXForwardedFor(){

        def principal = new UserDetails() {

            public Integer id = 1

            @Override
            Collection<GrantedAuthority> getAuthorities() {
                return null
            }
            @Override
            String getPassword() {
                return "test password"
            }
            @Override
            String getUsername() {
                return "test user"
            }
            @Override
            boolean isAccountNonExpired() {
                return true
            }
            @Override
            boolean isAccountNonLocked() {
                return true
            }
            @Override
            boolean isCredentialsNonExpired() {
                return true
            }
            @Override
            boolean isEnabled() {
                return true
            }
        }
        def authentication = new TestingAuthenticationToken(principal, [])
        authentication.setDetails(remoteAddress : "192.168.1.0")

        // test for default remote address (without x-forwarded-for header)
        MockHttpServletRequest request0 = new MockHttpServletRequest();
        logger.request = request0
        logger.onApplicationEvent(new AuthenticationSuccessEvent(authentication))
        assert SpringSecurityEvent.list()[0].remoteAddress == "192.168.1.0"

        // test for "x-forwarded-for"
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("x-forwarded-for", "192.168.1.1")
        logger.request = request1
        logger.onApplicationEvent(new AuthenticationSuccessEvent(authentication))
        assert SpringSecurityEvent.list()[1].remoteAddress == "192.168.1.1"

        // test for "x_forwarded_for"
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader("x_forwarded_for", "192.168.1.2")
        logger.request = request2
        logger.onApplicationEvent(new AuthenticationSuccessEvent(authentication))
        assert SpringSecurityEvent.list()[2].remoteAddress == "192.168.1.2"

        // test for "X-Forwarded-For"
        MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.addHeader("X-Forwarded-For", "192.168.1.3")
        logger.request = request3
        logger.onApplicationEvent(new AuthenticationSuccessEvent(authentication))
        assert SpringSecurityEvent.list()[3].remoteAddress == "192.168.1.3"

        // test for "X_Forwarded_For"
        MockHttpServletRequest request4 = new MockHttpServletRequest();
        request4.addHeader("X_Forwarded_For", "192.168.1.4")
        logger.request = request4
        logger.onApplicationEvent(new AuthenticationSuccessEvent(authentication))
        assert SpringSecurityEvent.list()[4].remoteAddress == "192.168.1.4"
    }

}
