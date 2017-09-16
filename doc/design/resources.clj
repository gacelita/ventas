;;
;; Un problema común en Wordpress es que se quiere subir una imagen estática
;; para usar en un plugin "Custom HTML", en una plantilla, o en otro sitio.
;; Estas imágenes tienen un significado semántico no documentado al hacerlo de
;; este modo.
;;
;; Propongo definir estos recursos en la base de datos. Deberían tener una keyword
;; única que se usará para hacer referencia a ellos. Estos recursos tendrán un :file
;; asociado (las entidades :file solo sirven para referirse a archivos de una forma
;; centralizada y no tienen ningún significado semántico).
;;
;; Opcionalmente se debería poder asociar un nombre a los recursos para mostrarlos
;; en la administración.
;; :resource resuelve el problema de inflexibilidad que tiene Prestashop al definir el
;; logo como algo único en lugar de una instancia de algo más genérico y útil.
;; Wordpress sufre también este problema (aunque no es tan insidioso debido a la existencia
;; del gestor de medios, análogo a nuestro :file, pero no tienen identificadores).

(db/entity-create :resource {:keyword :logo :file 1234567890})
(util/get-resource-url :logo) ;; la URL del :file asociado

;; Otro problema común en Prestashop y Wordpress es el siguiente: el usuario quiere
;; insertar HTML ad-hoc, por ejemplo para añadir un teléfono de contacto o un elemento
;; de SEO (call-to-action, etc.). Las 3 plataformas resuelven este problema con distintos
;; grados de éxito:

;; -- Prestashop --
;; Probablemente el peor de todos, ya que incluye varios módulos que podrían haber sido
;; implementados con un módulo de HTML. Por supuesto esto no es posible con Prestashop
;; ya que no puede haber más de una instancia de un módulo en un hook (lo cual es un grave
;; fallo de diseño, aún peor que el hecho de que los módulos tengan que especificar qué
;; hooks soportan)

;; -- Wordpress --
;; Wordpress soluciona este problema de una forma aceptable, con sus widgets de HTML.
;; Sin embargo lo único que obtiene el usuario es un simple textarea que no tiene
;; acceso a las APIs de WP, con lo cual todo el contenido es plenamente estático, dando
;; lugar a problemas cuando las URLs de los archivos cambian, el nombre de la tienda cambia,
;; etc.

;; Lo que propongo para ventas es arreglar el sistema propuesto por Wordpress. Ya que
;; en ventas el HTML se especifica con hiccup, el usuario ya tiene que escribir código
;; de todos modos, así que lo ideal sería que el entorno en el que se ejecutase dicho
;; código tuviese acceso a la API de ventas (es decir, todos los namespaces), para poder
;; insertar código como:
[:div.custom.some-component
 [:a {:href (util/get-resource-url :some-resource)}]]
;; Esto no es inseguro, ya que el usuario que introduce este código ya tiene permisos
;; de administrador de todos modos (es decir, es su responsabilidad).

;; Estos componentes personalizados pueden incluir otros componentes definidos por el usuario,
;; o bien otros componentes incluidos con ventas:
[:div.custom.some-component
  [ventas.plugins.featured-products/featured-products]]

;; Aunque dichos componentes pueden ser editados mediante la interfaz web, creo que es mejor
;; que sean guardados como fichero, para poder editarlos con un editor de texto si al
;; administrador le place. Esto es otro problema en Wordpress: los widgets son serializados
;; y guardados en la base de datos, lo cual dificulta enormemente los cambios masivos (que de todos
;; modos no serían tan comunes en ventas, pero sigue siendo mejor idea)

;; El usuario debería dar un identificador a los widgets personalizados que cree.
;; Los widgets tendrán esta estructura de ficheros:
;; ventas
;;   cljs
;;     widgets
;;       [identifier].clj

;; Otro problema en Wordpress es que los widgets son asociados a "sidebars" y dichos "sidebars" son
;; mostrados por la plantilla en el lugar que escoja el creador de la plantilla. Esto limita mucho
;; al usuario, ya que los widgets tienen que ir estrictamente uno después de otro, y el usuario no
;; puede escoger dónde acabarán colocados. Dado que nuestros widgets pueden incluir a otros widgets
;; realmente no hay necesidad de usar un sistema como ese. ventas debería ofrecer widgets que el usuario
;; puede combinar junto con sus propios widgets para definir la layout de su tienda.

;; El routing en WP en general también me parece terrible. En ventas, la plantilla define unas rutas
;; base, y luego el usuario puede editar sus URL y añadir nuevas rutas.

;; El usuario puede crear sus propias páginas simplemente creando un widget, una ruta, y asociándolos.

;; Así que básicamente ventas ofrece unos componentes base, y el usuario puede crear sus componentes, y
;; si quiere puede asociarlos a rutas. Esto está cerca del funcionamiento real de ventas (se asocia
;; un componente a una ruta mediante un multimétodo). Realmente las páginas no tienen nada de especial,
;; solo que están asociadas a una ruta. Los "skeletons" son solo componentes que son usados por componentes
;; asociados a rutas, nada especial sobre esos tampoco.

;; De momento tenemos lo siguiente: ventas está hecho de rutas y componentes asociados. ventas ofrece
;; unas rutas, pero el usuario puede editarlas y crear nuevas. ventas ofrece también componentes, pero
;; cómo estos se combinan queda a elección del usuario, quien además puede crear nuevos componentes.

;; Para extender la API (es decir, la parte de Clojure), es necesario crear plugins. Esto es algo que
;; Prestashop hace realmente mal: obliga a crear módulos para cosas triviales como añadir HTML
;; a una parte de la página. En wordpress no es tan terrible pero algunos módulos sí que podrían
;; ser reemplazados por templating y llamadas a API.

;; Así que realmente solo hay un montón de componentes, las páginas "no existen". Esto debería
;; ser refactorizado. En todo caso podría haber un ns de componentes que se llame "pages", ya que
;; realmente son componentes pensados para ser páginas.

;; Es posible extender ventas con:
;; - Componentes: estrictamente no extienden la API
;; - Plugins: pueden incluir componentes y extender la API
;; - Plantillas: técnicamente las plantillas solo deberían ser un montón de componentes
;;   pero todo el mundo sabe que las plantillas quieren extender la API, Prestashop lo hace
;;   con overrides (terrible) y WP simplemente se expone como una API que es usada por la
;;   plantilla. ventas se parece algo a WP en el sentido de que en WP los archivos de la
;;   plantilla definen la presentación (en HTML) y usan la API de WP como ayuda. En ventas
;;   esto son componentes, y están dentro de la plantilla, pero el problema de WP es que cualquier
;;   extensión a la API ha de realizarse en functions.php. En ventas esto no es un problema,
;;   las plantillas incluyen plugins y componentes. Para evitar que el usuario pierda control
;;   sobre la plantilla (cosa que pasa mucho en WP) el usuario siempre va a poder editar los
;;   componentes de la plantilla, así como sus rutas (definidas por la plantilla en WP).

;; Otra cosa que es preocupante es la extensión de la base de datos. Datomic no permite eliminar
;; nunca atributos, esto debería tenerse en cuenta. Lo que sí permite (y esto es absolutamente
;; fantástico) es usar ns estilo Clojure para definir los atributos. En entornos tradicionales
;; hay dos formas comunes de extender la db: extendiendo una tabla existente o creando una.
;; Gracias a Datomic, en ventas no hay diferencia:
:plugins.featured-products.product/featured
.plugins.quotes.quote/name

;; Sin embargo, los plugins deben definir su nuevo tipo como parte del enum schema.type:
:schema.type :plugins.quote/quote

;; El problema con los recursos
;; Tanto los componentes como los plugins como las plantillas deberían poder incluir sus propios recursos.
;; Se debería extender ventas a través de una sola entidad: los paquetes (packages). Los paquetes contienen
;; cualquier número de plugins, componentes, rutas y recursos. Esto elimina las plantillas y centraliza la
;; extensión.
