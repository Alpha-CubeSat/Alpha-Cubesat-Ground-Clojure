(ns control-frontend.command-macros)

(defmacro defcommand
  "Creates a new command with name, title, description,
  backend type key, and argument fields"
  [name title desc backend-type fields]
  `(def ^:const ~name
     {:title        ~title
      :description  ~desc
      :backend-type ~backend-type
      :fields       ~fields}))

(defmacro defcategory
  "Creates a category of commands containing the name,
  keyword name, display title, and specified commands"
  [name name-kw title commands]
  `(def ^:const ~name
     {:category ~name-kw
      :title ~title
      :commands ~commands}))
