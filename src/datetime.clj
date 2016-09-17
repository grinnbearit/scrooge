(ns datetime)


(defmethod print-dup org.joda.time.LocalDate
  [o ^java.io.Writer writer]
  (.write writer (format "#date \"%s\"" o)))


(defmethod print-method org.joda.time.LocalDate
  [o ^java.io.Writer writer]
  (.write writer (format "#date \"%s\"" o)))
