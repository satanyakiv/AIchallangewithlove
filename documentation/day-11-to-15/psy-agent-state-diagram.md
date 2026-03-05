stateDiagram-v2
[*] --> Greeting : startSession()

    Greeting --> ActiveListening : UserMessage len over 10
    Greeting --> CrisisMode : CrisisDetected

    state ActiveListening {
        direction LR
        [*] --> Listening
        Listening --> Listening : UserMessage continue
    }

    ActiveListening --> Intervention : TechniqueAccepted
    ActiveListening --> Closing : SessionTimeUp
    ActiveListening --> Closing : UserRequestedEnd
    ActiveListening --> CrisisMode : CrisisDetected

    state Intervention {
        direction LR
        [*] --> Step1
        Step1 --> Step2 : NextStep
        Step2 --> Step3 : NextStep
    }

    Intervention --> ActiveListening : TechniqueCompleted
    Intervention --> Closing : UserRequestedEnd
    Intervention --> CrisisMode : CrisisDetected

    state CrisisMode {
        direction LR
        [*] --> Assessing
        Assessing --> DeEscalating : indicators cleared
        DeEscalating --> Resolved : >5 min stable
    }

    CrisisMode --> ActiveListening : RiskResolved

    Closing --> Finished : SummaryDelivered
    Closing --> ActiveListening : UserRaisedNewTopic

    Finished --> [*]

    note right of CrisisMode
        GUARDS:
        Cannot exit before 5 min
        Cannot transition to Closing
        All other transitions blocked
    end note

    note right of Finished
        TERMINAL STATE
        No outgoing transitions
        New message = new session
    end note

    note left of Closing
        User can still respond
        and return to ActiveListening
    end note