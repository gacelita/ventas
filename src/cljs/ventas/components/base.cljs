(ns ventas.components.base
  (:require
   [reagent.core :as reagent]
   [soda-ash.core :as sa]
   [ventas.i18n :refer [i18n]]
   [ventas.common.utils :as common.utils])
  (:refer-clojure :exclude [list comment]))

(def accordion sa/Accordion)
(def accordion-content sa/AccordionContent)
(def accordion-title sa/AccordionTitle)
(def advertisement sa/Advertisement)
(def breadcrumb sa/Breadcrumb)
(def breadcrumb-divider sa/BreadcrumbDivider)
(def breadcrumb-section sa/BreadcrumbSection)
(def button sa/Button)
(def button-content sa/ButtonContent)
(def button-group sa/ButtonGroup)
(def button-or sa/ButtonOr)
(def card sa/Card)
(def card-content sa/CardContent)
(def card-description sa/CardDescription)
(def card-group sa/CardGroup)
(def card-header sa/CardHeader)
(def card-meta sa/CardMeta)
(def checkbox sa/Checkbox)
(def comment sa/CommentSA)
(def comment-action sa/CommentAction)
(def comment-actions sa/CommentActions)
(def comment-author sa/CommentAuthor)
(def comment-avatar sa/CommentAvatar)
(def comment-content sa/CommentContent)
(def comment-group sa/CommentGroup)
(def comment-metadata sa/CommentMetadata)
(def comment-text sa/CommentText)
(def confirm sa/Confirm)
(def container sa/Container)
(def dimmer sa/Dimmer)
(def dimmer-dimmable sa/DimmerDimmable)
(def divider sa/Divider)

(defn dropdown [options & [child]]
  [sa/Dropdown
   (-> options
       (common.utils/update-when-some
        :default-value
        (fn [v]
          (if (coll? v)
            (map str v)
            (str v))))
       (common.utils/update-when-some
        :options
        (fn [options]
          (map (fn [option]
                 (if (map? option)
                   (update option :value str)
                   option))
               options))))
   child])

(def dropdown-divider sa/DropdownDivider)
(def dropdown-header sa/DropdownHeader)
(def dropdown-item sa/DropdownItem)
(def dropdown-menu sa/DropdownMenu)
(def embed sa/Embed)
(def feed sa/Feed)
(def feed-content sa/FeedContent)
(def feed-date sa/FeedDate)
(def feed-event sa/FeedEvent)
(def feed-extra sa/FeedExtra)
(def feed-label sa/FeedLabel)
(def feed-like sa/FeedLike)
(def feed-meta sa/FeedMeta)
(def feed-summary sa/FeedSummary)
(def feed-user sa/FeedUser)
(def flag sa/Flag)
(def form sa/Form)
(def form-button sa/FormButton)
(def form-checkbox sa/FormCheckbox)
(def form-dropdown sa/FormDropdown)
(def form-field sa/FormField)
(def form-group sa/FormGroup)
(def form-input sa/FormInput)
(def form-radio sa/FormRadio)
(def form-select sa/FormSelect)
(def form-textarea sa/FormTextArea)
(def grid sa/Grid)
(def grid-column sa/GridColumn)
(def grid-row sa/GridRow)
(def header sa/Header)
(def header-content sa/HeaderContent)
(def header-subheader sa/HeaderSubheader)
(def icon sa/Icon)
(def icon-group sa/IconGroup)
(def image sa/Image)
(def image-group sa/ImageGroup)
(def input sa/Input)
(def item sa/Item)
(def item-content sa/ItemContent)
(def item-description sa/ItemDescription)
(def item-extra sa/ItemExtra)
(def item-group sa/ItemGroup)
(def item-header sa/ItemHeader)
(def item-image sa/ItemImage)
(def item-meta sa/ItemMeta)
(def label sa/Label)
(def label-detail sa/LabelDetail)
(def label-group sa/LabelGroup)
(def list sa/ListSA)
(def list-content sa/ListContent)
(def list-description sa/ListDescription)
(def list-header sa/ListHeader)
(def list-icon sa/ListIcon)
(def list-item sa/ListItem)
(def list-list sa/ListList)
(def loader sa/Loader)
(def menu sa/Menu)
(def menu-header sa/MenuHeader)
(def menu-item sa/MenuItem)
(def menu-menu sa/MenuMenu)
(def message sa/Message)
(def message-content sa/MessageContent)
(def message-header sa/MessageHeader)
(def message-item sa/MessageItem)
(def message-list sa/MessageList)
(def modal sa/Modal)
(def modal-actions sa/ModalActions)
(def modal-content sa/ModalContent)
(def modal-description sa/ModalDescription)
(def modal-header sa/ModalHeader)
(def popup sa/Popup)
(def popup-content sa/PopupContent)
(def popup-header sa/PopupHeader)
(def portal sa/Portal)
(def progress sa/Progress)
(def radio sa/Radio)
(def rail sa/Rail)
(def rating sa/Rating)
(def rating-icon sa/RatingIcon)
(def reveal sa/Reveal)
(def reveal-content sa/RevealContent)
(def search sa/Search)
(def search-category sa/SearchCategory)
(def search-result sa/SearchResult)
(def search-results sa/SearchResults)

(defn- semantic->css-color [color]
  (get {"red" "#DB2828"
        "orange" "#F2711C"
        "yellow" "#FBBD08"
        "olive" "#B5CC18"
        "green" "#21BA45"
        "teal" "#00B5AD"
        "blue" "#2185D0"
        "violet" "#6435C9"
        "purple" "#A333C8"
        "pink" "#E03997"
        "grey" "#767676"
        "black" "#1B1C1D"}
       color))

(defn segment [{:keys [title color] :as opts} & children]
  [sa/Segment (if-not title
                opts
                (update opts :class #(str % "segment--with-title")))
   (when title
     [:div.segment-title {:style {:background-color (semantic->css-color color)}}
      title])
   (map-indexed
    (fn [idx child]
      (with-meta child {:key idx}))
    children)])

(def segment-group sa/SegmentGroup)
(def select sa/Select)
(def sidebar sa/Sidebar)
(def sidebar-pushable sa/SidebarPushable)
(def sidebar-pusher sa/SidebarPusher)
(def statistic sa/Statistic)
(def statistic-group sa/StatisticGroup)
(def statistic-label sa/StatisticLabel)
(def statistic-value sa/StatisticValue)
(def step sa/Step)
(def step-content sa/StepContent)
(def step-description sa/StepDescription)
(def step-group sa/StepGroup)
(def step-title sa/StepTitle)
(def table sa/Table)
(def table-body sa/TableBody)
(def table-cell sa/TableCell)
(def table-footer sa/TableFooter)
(def table-header sa/TableHeader)
(def table-header-cell sa/TableHeaderCell)
(def table-row sa/TableRow)
(def text-area sa/TextArea)
(def visibility sa/Visibility)

(defn loading []
  [dimmer {:active true
           :inverted true}
   [loader {:inverted true}
    (i18n ::loading)]])