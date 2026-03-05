sequenceDiagram
actor U as User
participant A as AgentPipeline
participant SM as StateMachine
participant LLM as LLM
participant V as InvariantChecker
participant CS as ContextStore

    Note over U,CS: SESSION START

    U->>A: startSession
    A->>CS: createSession
    CS-->>A: new SessionContext
    A->>SM: reset to Greeting
    A->>CS: loadUserProfile
    CS-->>A: UserProfile

    Note over U,CS: GREETING

    U->>A: Hi, I have been feeling anxious lately
    A->>CS: assembleContext
    CS-->>A: AgentContext

    rect rgb(230, 245, 255)
        Note over A: PLAN
        A->>A: intent = ACKNOWLEDGE_AND_REFLECT
        A->>A: transition = Greeting to ActiveListening
    end

    rect rgb(230, 255, 230)
        Note over A,LLM: EXECUTE
        A->>LLM: systemPrompt + userProfile + message
        LLM-->>A: Hi, tell me more about that anxiety...
    end

    rect rgb(255, 245, 230)
        Note over A,V: VALIDATE
        A->>V: check response and context
        V-->>A: Passed
    end

    rect rgb(240, 240, 255)
        Note over A,CS: DONE
        A->>SM: transition to ActiveListening
        A->>CS: appendMessage
        A->>CS: updateSessionAnalytics
    end

    A-->>U: Hi, tell me more about that anxiety...

    Note over U,CS: ACTIVE LISTENING

    U->>A: Work pressure is constant, I cannot sleep
    A->>CS: assembleContext
    CS-->>A: context with turnCount=1

    rect rgb(230, 245, 255)
        Note over A: PLAN
        A->>A: intent = ASK_CLARIFYING_QUESTION
        A->>A: detected emotions: stress, insomnia
    end

    rect rgb(230, 255, 230)
        Note over A,LLM: EXECUTE
        A->>LLM: systemPrompt for ActiveListening turn 2
        LLM-->>A: That sounds exhausting. When did this start?
    end

    A->>V: check response
    V-->>A: Passed
    A->>SM: self-loop ActiveListening turn=2
    A->>CS: save
    A-->>U: That sounds exhausting. When did this start?

    Note right of A: 3-4 more turns of active listening

    U->>A: I have been struggling with this for a month

    rect rgb(230, 245, 255)
        Note over A: PLAN
        A->>A: turnCount=5 enough context
        A->>A: intent = SUGGEST_TECHNIQUE
        A->>A: selected Progressive Muscle Relaxation
    end

    A->>LLM: prompt to suggest technique
    LLM-->>A: I noticed a tension pattern. Want to try a relaxation technique?
    A->>V: check
    V-->>A: Passed
    A-->>U: Want to try a relaxation technique?

    Note over U,CS: INTERVENTION

    U->>A: Yes lets try it

    A->>SM: transition to Intervention

    loop Technique Steps x3
        A->>LLM: prompt for Intervention step N
        LLM-->>A: instruction for step N
        A->>V: check
        V-->>A: Passed
        A-->>U: Step N instruction
        U->>A: done with step
        A->>SM: self-loop Intervention next step
    end

    A->>SM: transition to ActiveListening

    Note over U,CS: CLOSING

    U->>A: Thanks I feel better. I want to finish.

    A->>SM: transition to Closing

    rect rgb(230, 245, 255)
        Note over A: PLAN
        A->>A: intent = SUMMARIZE_AND_CLOSE
        A->>A: gather topics and techniques used
    end

    A->>LLM: prompt for Closing with session analytics
    LLM-->>A: Session summary with homework
    A->>V: check
    V-->>A: Passed

    A->>CS: saveSessionSummary
    A->>CS: updateUserProfile
    A-->>U: Summary and homework

    Note over U,CS: FINISHED

    U->>A: Thanks see you next time

    A->>SM: transition to Finished
    A->>CS: finalizeSession

    Note over SM: TERMINAL STATE
    Note over SM: New message starts new session