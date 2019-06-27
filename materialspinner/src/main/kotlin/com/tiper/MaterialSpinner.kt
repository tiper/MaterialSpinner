package com.tiper

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.content.res.Resources
import android.database.DataSetObserver
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.ListPopupWindow
import android.text.InputType
import android.util.AttributeSet
import android.view.SoundEffectConstants
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.AdapterView
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SpinnerAdapter
import android.widget.ThemedSpinnerAdapter
import com.tiper.materialspinner.R

/**
 * Layout which wraps an [TextInputEditText] to show a floating label when the hint is hidden due to
 * the user inputting text.
 *
 * @see [TextInputLayout]
 * @author Tiago Pereira (tiagomiguelmoreirapereira@gmail.com)
 */
open class MaterialSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    mode: Int = MODE_DROPDOWN
) : TextInputLayout(context, attrs) {

    companion object {
        /**
         * Represents an invalid position.
         * All valid positions are in the range 0 to 1 less than the number of items in the current
         * adapter.
         */
        const val INVALID_POSITION = -1

        /**
         * Use a dialog window for selecting spinner options.
         */
        const val MODE_DIALOG = 0

        /**
         * Use a dropdown anchored to the Spinner for selecting spinner options.
         */
        const val MODE_DROPDOWN = 1

        /**
         * Use a bottom sheet dialog window for selecting spinner options.
         */
        const val MODE_BOTTOMSHEET = 2
    }

    /**
     * The view that will display the available list of choices.
     */
    private val popup: SpinnerPopup

    /**
     * The view that will display the selected item.
     */
    private val editText = TextInputEditText(getContext())

    /**
     * Extended [android.widget.Adapter] that is the bridge between this Spinner and its data.
     */
    var adapter: SpinnerAdapter? = null
        set(value) {
            field = DropDownAdapter(value, context.theme).also {
                popup.setAdapter(it)
            }
        }

    /**
     * The listener that receives notifications when an item is selected.
     */
    var onItemSelectedListener: OnItemSelectedListener? = null

    /**
     * The listener that receives notifications when an item is clicked.
     */
    var onItemClickListener: OnItemClickListener? = null

    /**
     * The currently selected item.
     */
    var selection = INVALID_POSITION
        set(value) {
            field = value
            adapter?.apply {
                if (value in 0 until count) {
                    editText.setText(
                        when (val item = getItem(value)) {
                            is CharSequence -> item
                            else -> item.toString()
                        }
                    )
                    onItemSelectedListener?.onItemSelected(
                        this@MaterialSpinner,
                        null,
                        value,
                        getItemId(value)
                    )
                } else {
                    editText.setText("")
                    onItemSelectedListener?.onNothingSelected(this@MaterialSpinner)
                }
            }
        }
    /**
     * Sets the [prompt] to display when the dialog is shown.
     *
     * @return The prompt to display when the dialog is shown.
     */
    var prompt: CharSequence?
        set(value) {
            popup.setPromptText(value)
        }
        get() = popup.getPrompt()

    /**
     * @return The data corresponding to the currently selected item, or null if there is nothing
     * selected.
     */
    val selectedItem: Any?
        get() = popup.getItem(selection)

    /**
     * @return The id corresponding to the currently selected item, or {@link #INVALID_ROW_ID} if
     * nothing is selected.
     */
    val selectedItemId: Long
        get() = popup.getItemId(selection)

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MaterialSpinner).run {
            editText.isEnabled =
                getBoolean(R.styleable.MaterialSpinner_android_enabled, editText.isEnabled)
            editText.isFocusable =
                getBoolean(R.styleable.MaterialSpinner_android_focusable, editText.isFocusable)
            editText.isFocusableInTouchMode = getBoolean(
                R.styleable.MaterialSpinner_android_focusableInTouchMode,
                editText.isFocusableInTouchMode
            )
            getColorStateList(R.styleable.MaterialSpinner_android_textColor)?.let {
                editText.setTextColor(
                    it
                )
            }
            getDimensionPixelSize(
                R.styleable.MaterialSpinner_android_textSize,
                -1
            ).let { if (it > 0) editText.textSize = it.toFloat() }
            getText(R.styleable.MaterialSpinner_android_text)?.let {
                // Allow text in debug mode for preview purposes.
                if (isInEditMode) {
                    editText.setText(it)
                } else {
                    throw RuntimeException("Don't set text directly." +
                            "You probably want setSelection instead.")
                }
            }
            popup = when (getInt(R.styleable.MaterialSpinner_spinnerMode, mode)) {
                MODE_DIALOG -> {
                    DialogPopup(context, getString(R.styleable.MaterialSpinner_android_prompt))
                }
                MODE_BOTTOMSHEET -> {
                    BottomSheetPopup(context, getString(R.styleable.MaterialSpinner_android_prompt))
                }
                else -> {
                    DropdownPopup(context, attrs)
                }
            }

            // Create the color state list.
            //noinspection Recycle
            context.obtainStyledAttributes(
                attrs,
                intArrayOf(R.attr.colorControlActivated, R.attr.colorControlNormal)
            ).run {
                val activated = getColor(0, 0)
                @SuppressLint("ResourceType")
                val normal = getColor(1, 0)
                recycle()
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_pressed),
                        intArrayOf(android.R.attr.state_focused),
                        intArrayOf()
                    ), intArrayOf(activated, activated, normal)
                )
            }.let {
                // Set the arrow and properly tint it.
                getContext().getDrawableCompat(
                    getResourceId(
                        R.styleable.MaterialSpinner_android_src,
                        getResourceId(
                            R.styleable.MaterialSpinner_srcCompat,
                            R.drawable.ic_spinner_drawable
                        )
                    ), getContext().theme
                )?.apply {
                    DrawableCompat.setTintList(this, it)
                    DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_IN)
                }
            }?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }.also {
                setDrawable(it)
            }

            recycle()
        }

        this.addView(editText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        popup.setOnDismissListener(object : SpinnerPopup.OnDismissListener {
            override fun onDismiss() {
                editText.clearFocus()
            }
        })

        // Disable input.
        editText.maxLines = 1
        editText.inputType = InputType.TYPE_NULL

        editText.setOnClickListener {
            popup.show(selection)
        }

        editText.onFocusChangeListener.let {
            editText.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                v.handler.post {
                    if (hasFocus) {
                        v.performClick()
                    }
                    it?.onFocusChange(v, hasFocus)
                }
            }
        }
    }

    private fun setDrawable(d: Drawable?) {
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, d, null)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        throw RuntimeException("Don't call setOnClickListener." +
                    "You probably want setOnItemClickListener instead."
        )
    }

    /**
     * Set whether this view can receive the focus.
     * Setting this to false will also ensure that this view is not focusable in touch mode.
     *
     * @param [focusable] If true, this view can receive the focus.
     *
     * @see [android.view.View.setFocusableInTouchMode]
     * @see [android.view.View.setFocusable]
     * @attr ref android.R.styleable#View_focusable
     */
    override fun setFocusable(focusable: Boolean) {
        editText.isFocusable = focusable
        super.setFocusable(focusable)
    }

    /**
     * Set whether this view can receive focus while in touch mode.
     * Setting this to true will also ensure that this view is focusable.
     *
     * @param [focusableInTouchMode] If true, this view can receive the focus while in touch mode.
     *
     * @see [android.view.View.setFocusable]
     * @attr ref android.R.styleable#View_focusableInTouchMode
     */
    override fun setFocusableInTouchMode(focusableInTouchMode: Boolean) {
        editText.isFocusableInTouchMode = focusableInTouchMode
        super.setFocusableInTouchMode(focusableInTouchMode)
    }

    /**
     * Call the OnItemClickListener, if it is defined.
     * Performs all normal actions associated with clicking: reporting accessibility event, playing
     * a sound, etc.
     *
     * @param [view] The view within the adapter that was clicked.
     * @param [position] The position of the view in the adapter.
     * @param [id] The row id of the item that was clicked.
     * @return True if there was an assigned OnItemClickListener that was called, false otherwise is
     * returned.
     */
    fun performItemClick(view: View?, position: Int, id: Long): Boolean {
        return run {
            onItemClickListener?.let {
                playSoundEffect(SoundEffectConstants.CLICK)
                it.onItemClick(this@MaterialSpinner, view, position, id)
                true
            } ?: false
        }.also {
            view?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        }
    }

    /**
     * Sets the prompt to display when the dialog is shown.
     *
     * @param [promptId] the resource ID of the prompt to display when the dialog is shown.
     */
    fun setPromptId(promptId: Int) {
        prompt = context.getText(promptId)
    }

    private fun Context.getDrawableCompat(
        @DrawableRes id: Int,
        theme: Resources.Theme?
    ): Drawable? {
        return resources.getDrawableCompat(id, theme)
    }

    private fun Resources.getDrawableCompat(
        @DrawableRes id: Int,
        theme: Resources.Theme?
    ): Drawable? {
        return ResourcesCompat.getDrawable(this, id, theme)?.let { DrawableCompat.wrap(it) }
    }

    private inner class DialogPopup(
        val context: Context,
        private var prompt: CharSequence? = null
    ) : DialogInterface.OnClickListener, SpinnerPopup {

        private var popup: AlertDialog? = null
        private var adapter: ListAdapter? = null
        private var listener: SpinnerPopup.OnDismissListener? = null

        override fun setAdapter(adapter: ListAdapter?) {
            this.adapter = adapter
        }

        override fun setPromptText(hintText: CharSequence?) {
            prompt = hintText
        }

        override fun getPrompt(): CharSequence? {
            return prompt
        }

        override fun show(position: Int) {
            if (adapter == null) {
                return
            }

            popup = adapter?.let { adapter ->
                AlertDialog.Builder(context).let { builder ->
                    prompt?.let { prompt ->
                        builder.setTitle(prompt)
                    }
                    builder.setSingleChoiceItems(adapter, position, this).create().apply {
                        popup?.listView?.let {
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                                it.textDirection = textDirection
                                it.textAlignment = textAlignment
                            }
                        }
                        setOnDismissListener { listener?.onDismiss() }
                    }
                }.also {
                    it.show()
                }
            }
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            this@MaterialSpinner.selection = which
            onItemClickListener?.let {
                this@MaterialSpinner.performItemClick(null, which, adapter?.getItemId(which) ?: 0L)
            }
            popup?.dismiss()
        }

        override fun setOnDismissListener(listener: SpinnerPopup.OnDismissListener?) {
            this.listener = listener
        }

        override fun getItem(position: Int): Any? {
            return adapter?.getItem(position)
        }

        override fun getItemId(position: Int): Long {
            return adapter?.getItemId(position) ?: INVALID_POSITION.toLong()
        }
    }

    /**
     * A PopupWindow that anchors itself to a host view and displays a list of choices.
     */
    @SuppressLint("RestrictedApi")
    private inner class DropdownPopup(context: Context, attrs: AttributeSet?) :
        ListPopupWindow(context, attrs), SpinnerPopup {

        init {
            inputMethodMode = INPUT_METHOD_NOT_NEEDED
            anchorView = this@MaterialSpinner
            isModal = true
            promptPosition = POSITION_PROMPT_ABOVE
            setOverlapAnchor(false)

            setOnItemClickListener { parent, v, position, id ->
                this@MaterialSpinner.selection = position
                onItemClickListener?.let {
                    this@MaterialSpinner.performItemClick(
                        v,
                        position,
                        adapter?.getItemId(position) ?: 0L
                    )
                }
                dismiss()
            }
        }

        override fun show(position: Int) {
            super.show()
            listView?.let {
                it.choiceMode = ListView.CHOICE_MODE_SINGLE
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    it.textDirection = textDirection
                    it.textAlignment = textAlignment
                }
            }
            setSelection(position)
        }

        override fun setOnDismissListener(listener: SpinnerPopup.OnDismissListener?) {
            super.setOnDismissListener {
                listener?.onDismiss()
            }
        }

        override fun setPromptText(hintText: CharSequence?) = Unit

        override fun getPrompt(): CharSequence? {
            return null
        }

        override fun getItem(position: Int): Any? {
            return adapter?.getItem(position)
        }

        override fun getItemId(position: Int): Long {
            return adapter?.getItemId(position) ?: INVALID_POSITION.toLong()
        }
    }

    private inner class BottomSheetPopup(
        val context: Context,
        private var prompt: CharSequence? = null
    ) : SpinnerPopup {

        private var popup: BottomSheetDialog? = null
        private var adapter: ListAdapter? = null
        private var listener: SpinnerPopup.OnDismissListener? = null

        override fun setAdapter(adapter: ListAdapter?) {
            this.adapter = adapter
        }

        override fun setPromptText(hintText: CharSequence?) {
            prompt = hintText
        }

        override fun getPrompt(): CharSequence? {
            return prompt
        }

        override fun show(position: Int) {
            if (adapter == null) {
                return
            }

            popup = BottomSheetDialog(context).apply {
                prompt?.let { prompt ->
                    setTitle(prompt)
                }
                setContentView(ListView(context).apply {
                    adapter = this@BottomSheetPopup.adapter

                    onItemClickListener =
                        AdapterView.OnItemClickListener { parent, v, position, id ->
                            this@MaterialSpinner.selection = position
                            onItemClickListener?.let {
                                this@MaterialSpinner.performItemClick(
                                    v,
                                    position,
                                    adapter?.getItemId(position) ?: 0L
                                )
                            }
                            dismiss()
                        }
                })
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    textDirection = this@MaterialSpinner.textDirection
                    textAlignment = this@MaterialSpinner.textAlignment
                }
                setOnDismissListener { listener?.onDismiss() }
            }.also {
                it.show()
            }
        }

        override fun setOnDismissListener(listener: SpinnerPopup.OnDismissListener?) {
            this.listener = listener
        }

        override fun getItem(position: Int): Any? {
            return adapter?.getItem(position)
        }

        override fun getItemId(position: Int): Long {
            return adapter?.getItemId(position) ?: INVALID_POSITION.toLong()
        }
    }

    /**
     * Creates a new ListAdapter wrapper for the specified adapter.
     *
     * @param [adapter] The SpinnerAdapter to transform into a ListAdapter.
     * @param [dropDownTheme] The theme against which to inflate drop-down views, may be {@null}
     * to use default theme.
     */
    private inner class DropDownAdapter(
        private val adapter: SpinnerAdapter?,
        dropDownTheme: Resources.Theme?
    ) : ListAdapter, SpinnerAdapter {

        private val listAdapter: ListAdapter?

        init {

            listAdapter = when (val it = adapter) {
                is ListAdapter -> it
                else -> null
            }

            dropDownTheme?.let {
                when (adapter) {
                    is android.support.v7.widget.ThemedSpinnerAdapter -> {
                        if (adapter.dropDownViewTheme != it) {
                            adapter.dropDownViewTheme = it
                        }
                    }
                    else -> {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                            when (adapter) {
                                is ThemedSpinnerAdapter -> {
                                    if (adapter.dropDownViewTheme == null) {
                                        adapter.dropDownViewTheme = it
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun getCount(): Int {
            return adapter?.count ?: 0
        }

        override fun getItem(position: Int): Any? {
            return adapter?.let {
                if (position > INVALID_POSITION && position < it.count) it.getItem(
                    position
                ) else null
            }
        }

        override fun getItemId(position: Int): Long {
            return adapter?.getItemId(position) ?: INVALID_POSITION.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            return getDropDownView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
            return adapter?.getDropDownView(position, convertView, parent)
        }

        override fun hasStableIds(): Boolean {
            return adapter?.hasStableIds() ?: false
        }

        override fun registerDataSetObserver(observer: DataSetObserver) {
            adapter?.registerDataSetObserver(observer)
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            adapter?.unregisterDataSetObserver(observer)
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call. Otherwise,
         * return true.
         */
        override fun areAllItemsEnabled(): Boolean {
            return listAdapter?.areAllItemsEnabled() ?: true
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call. Otherwise,
         * return true.
         */
        override fun isEnabled(position: Int): Boolean {
            return listAdapter?.isEnabled(position) ?: true
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun isEmpty(): Boolean {
            return count == 0
        }
    }

    /**
     * Interface for a callback to be invoked when an item in this view has been selected.
     */
    interface OnItemSelectedListener {
        /**
         * Callback method to be invoked when an item in this view has been selected.
         * This callback is invoked only when the newly selected position is different from the
         * previously selected position or if there was no selected item.
         * Implementers can call getItemAtPosition(position) if they need to access the data
         * associated with the selected item.
         *
         * @param [parent] The View where the selection happened.
         * @param [view] The view within the Adapter that was clicked.
         * @param [position] The position of the view in the adapter.
         * @param [id] The row id of the item that is selected.
         */
        fun onItemSelected(parent: MaterialSpinner, view: View?, position: Int, id: Long)

        /**
         * Callback method to be invoked when the selection disappears from this view.
         * The selection can disappear for instance when touch is activated or when the adapter
         * becomes empty.
         *
         * @param [parent] The View that now contains no selected item.
         */
        fun onNothingSelected(parent: MaterialSpinner)
    }

    /**
     * Interface definition for a callback to be invoked when an item in this View has been clicked.
     */
    interface OnItemClickListener {

        /**
         * Callback method to be invoked when an item in this View has been clicked.
         * Implementers can call getItemAtPosition(position) if they need to access the data
         * associated with the selected item.
         *
         * @param [parent] The View where the click happened.
         * @param [view] The view within the adapter that was clicked (this will be a view provided
         * by the adapter).
         * @param [position] The position of the view in the adapter.
         * @param [id] The row id of the item that was clicked.
         */
        fun onItemClick(parent: MaterialSpinner, view: View?, position: Int, id: Long)
    }

    /**
     * Implements some sort of popup selection interface for selecting a spinner option.
     * Allows for different spinner modes.
     */
    private interface SpinnerPopup {

        /**
         * Listener that is called when this popup window is dismissed.
         */
        interface OnDismissListener {
            /**
             * Called when this popup window is dismissed.
             */
            fun onDismiss()
        }

        /**
         * Set hint text to be displayed to the user. This should provide a description of the
         * choice being made.
         *
         * @param [hintText] Hint text to set.
         */
        fun setPromptText(hintText: CharSequence?)

        /**
         * @return The prompt to display when the dialog is shown
         */
        fun getPrompt(): CharSequence?

        /**
         * Sets the adapter that provides the data and the views to represent the data in this popup
         * window.
         *
         * @param [adapter] The adapter to use to create this window's content.
         */
        fun setAdapter(adapter: ListAdapter?)

        /**
         * Show the popup
         */
        fun show(position: Int)

        /**
         * Set a listener to receive a callback when the popup is dismissed.
         *
         * @param [listener] Listener that will be notified when the popup is dismissed.
         */
        fun setOnDismissListener(listener: OnDismissListener?)

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param [position] Position of the item whose data we want within the adapter's data set.
         * @return The data at the specified position.
         */
        fun getItem(position: Int): Any?

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param [position] The position of the item within the adapter's data set whose row id we
         * want.
         * @return The id of the item at the specified position.
         */
        fun getItemId(position: Int): Long
    }
}
