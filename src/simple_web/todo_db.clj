(ns simple-web.todo-db)

(defprotocol TaskDb
  (get-tasks [this] "Get tasks")
  (delete-task [this id] "Delete task by id")
  (update-task [this task] "Update task")
  (create-task [this title description] "Create task"))