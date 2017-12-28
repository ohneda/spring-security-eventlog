package ca.redtoad.eventlog

class SpringSecurityEvent {

    Integer userId = 0
    String username
    String sessionId
    String eventName
    String remoteAddress
    String switchedUsername
    Date dateCreated
    String userAgent
    String authorizationToken

    static constraints = {
        userId(nullable: false)
        username(nullable: true)
        sessionId(nullable: true)
        eventName()
        remoteAddress(nullable: true)
        switchedUsername(nullable: true)
        dateCreated()
        userAgent nullable: true
        authorizationToken nullable: true
    }

    static mapping = {
        version false
    }
}
