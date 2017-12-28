package ca.redtoad.eventlog

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

import javax.servlet.ServletException
import javax.servlet.http.*
import org.apache.commons.logging.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AbstractAuthenticationEvent
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent
import org.springframework.security.authentication.RememberMeAuthenticationToken

class SpringSecurityEventLogger implements ApplicationListener<AbstractAuthenticationEvent>, LogoutHandler {

    @Autowired
    private HttpServletRequest request
 
    private static final log = LogFactory.getLog(this)

    void logAuthenticationEvent(String eventName, Authentication authentication, String remoteAddress, String switchedUsername) {

        def authorizationToken = authentication?.class?.simpleName

        try {
            def username = authentication?.principal?.hasProperty('username')?.getProperty(authentication?.principal) ?: authentication?.principal
            def sessionId = authentication?.details?.sessionId
            def userId = ( authentication?.principal?.hasProperty("id") ? authentication?.principal?.id : 0 ) ?: 0

            def userAgent = request?.getHeader( 'user-agent' )

            if(userAgent.size() > 255){
                log.warn("Long UserAgent: ${userAgent}" )
                userAgent = userAgent(0,255)
            }

            if(username.size() > 255){
                log.warn("Long username: ${username}")
            }


            SpringSecurityEvent.withTransaction {
                def event = new SpringSecurityEvent(userId: userId,
                                                    username: username,
                                                    eventName: eventName,
                                                    sessionId: sessionId,
                                                    remoteAddress: remoteAddress,
                                                    switchedUsername: switchedUsername,
                                                    userAgent: userAgent,
                                                    authorizationToken: authorizationToken)
                event.save(failOnError:true)
            }
        } catch (RuntimeException e) {
            log.error("error saving spring security event", e)
        }
    }

    void onApplicationEvent(AbstractAuthenticationEvent event) {
        String remoteAddress = request.getHeader("x-forwarded-for") ?:request.getHeader("x_forwarded_for")?:event.authentication?.details?.remoteAddress
        logAuthenticationEvent(event.class.simpleName, event.authentication, remoteAddress, switchedUsername(event))
    }

    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        logAuthenticationEvent('Logout', authentication, request.remoteHost, null)
    }

    private static String switchedUsername(AbstractAuthenticationEvent event) {
        if (event instanceof AuthenticationSwitchUserEvent) {
            event.targetUser.username
        } else {
            null
        }
    }

}
