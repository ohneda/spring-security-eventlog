package ca.redtoad.eventlog

class SpringSecurityEvent {

    Integer userId = 0
    String username
    String sessionId
    String eventName
    String remoteAddress
    String switchedUsername
    Date dateCreated

    static constraints = {
        userId(nullable: false)
        username(nullable: true)
        sessionId(nullable: true)
        eventName()
        remoteAddress(nullable: true)
        switchedUsername(nullable: true)
        dateCreated()
    }

    static mapping = {
        version false
    }
}
