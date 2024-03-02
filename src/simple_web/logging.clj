(ns simple-web.logging
  (:require [com.brunobonacci.mulog :as u])
  (:import
   (ch.qos.logback.classic Level Logger)
   (ch.qos.logback.classic.spi ILoggingEvent ThrowableProxy)
   (ch.qos.logback.core Appender AppenderBase)
   (org.slf4j LoggerFactory)))

(defn trace-wrap [handler]
  (fn [req]
    (u/trace
     ::request [:req (select-keys req [:request-method :uri])]
     (handler req))))

(def trace-middleware
  {:name ::tracing-middleware
   :wrap trace-wrap})

; Logging - route SLF4J logs to mulog via logback
; From: https://mbezjak.github.io/posts/routing-slf4j-events-to-mulog/

(defn- new-slf4j-to-mulog-appender ^Appender []
  (proxy [AppenderBase] []
    (append ^void [^ILoggingEvent event]
      (u/log ::slf4j
             :message (.getFormattedMessage event)
             :logger (.getLoggerName event)
             :level (str (.getLevel event))
             :thread-name (.getThreadName event)
             :exception (when-let [ex-proxy (.getThrowableProxy event)]
                          (.getThrowable ^ThrowableProxy ex-proxy))))))

(defn- setup-slf4j-to-mulog []
  (let [appender (new-slf4j-to-mulog-appender)
        logger-context (LoggerFactory/getILoggerFactory)
        ^Logger root-logger (LoggerFactory/getLogger Logger/ROOT_LOGGER_NAME)]
    ;; Remove the default appenders that logback installs by default when no
    ;; logback.xml is present. See:
    ;; https://logback.qos.ch/manual/configuration.html#auto_configuration
    (.detachAndStopAllAppenders root-logger)
    ;; Set the default logging level for all loggers
    (.setLevel root-logger Level/INFO)
    ;; Adding appender that forwards everything to mulog.
    (.setContext appender logger-context)
    (.start appender)
    (.addAppender root-logger appender)))

(defn- stop-slf4j-to-mulog []
  (let [^Logger root-logger (LoggerFactory/getLogger Logger/ROOT_LOGGER_NAME)]
    (.detachAndStopAllAppenders root-logger)))

(defn start-console-logging! []
  (setup-slf4j-to-mulog)
  (u/start-publisher! {:type :console}))

(defn stop-logging! [mulogger]
  (stop-slf4j-to-mulog)
  (mulogger))
