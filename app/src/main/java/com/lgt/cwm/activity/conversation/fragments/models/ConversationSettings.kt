package com.lgt.cwm.activity.conversation.fragments.models

import android.util.Log
import com.lgt.cwm.ui.adapter.mapping.MappingModel
import com.lgt.cwm.ui.adapter.mapping.MappingModelList
import com.lgt.cwm.ui.setting.ConversationSettingsIcon
import com.lgt.cwm.ui.setting.ConversationSettingsText

import androidx.annotation.CallSuper
import androidx.annotation.Px
import androidx.annotation.StringRes
import com.lgt.cwm.ui.setting.models.AsyncSwitch
import com.lgt.cwm.ui.setting.models.Button
import com.lgt.cwm.ui.setting.models.Space
import com.lgt.cwm.ui.setting.models.Text

fun configure(init: ConversationSettingsConfiguration.() -> Unit): ConversationSettingsConfiguration {
    val configuration = ConversationSettingsConfiguration()
    configuration.init()
    return configuration
}

class ConversationSettingsConfiguration {
    private val children = arrayListOf<MappingModel<*>>()

    fun customPref(customPreference: MappingModel<*>) {
        children.add(customPreference)
    }

    fun radioListPref(
        title: ConversationSettingsText,
        icon: ConversationSettingsIcon? = null,
        dialogTitle: ConversationSettingsText = title,
        isEnabled: Boolean = true,
        listItems: Array<String>,
        selected: Int,
        confirmAction: Boolean = false,
        onSelected: (Int) -> Unit
    ) {
        val preference = RadioListPreference(
            title = title,
            icon = icon,
            isEnabled = isEnabled,
            dialogTitle = dialogTitle,
            listItems = listItems,
            selected = selected,
            confirmAction = confirmAction,
            onSelected = onSelected
        )
        children.add(preference)
    }

    fun multiSelectPref(
        title: ConversationSettingsText,
        isEnabled: Boolean = true,
        listItems: Array<String>,
        selected: BooleanArray,
        onSelected: (BooleanArray) -> Unit
    ) {
        val preference = MultiSelectListPreference(title, isEnabled, listItems, selected, onSelected)
        children.add(preference)
    }

    fun asyncSwitchPref(
        title: ConversationSettingsText,
        isEnabled: Boolean = true,
        isChecked: Boolean,
        isProcessing: Boolean,
        onClick: () -> Unit
    ) {
        val preference = AsyncSwitch.Model(title, isEnabled, isChecked, isProcessing, onClick)
        children.add(preference)
    }

    fun switchPref(
        title: ConversationSettingsText,
        summary: ConversationSettingsText? = null,
        icon: ConversationSettingsIcon? = null,
        isEnabled: Boolean = true,
        isChecked: Boolean,
        onClick: () -> Unit
    ) {
        val preference = SwitchPreference(title, summary, icon, isEnabled, isChecked, onClick)
        children.add(preference)
    }

    fun radioPref(
        title: ConversationSettingsText,
        summary: ConversationSettingsText? = null,
        isEnabled: Boolean = true,
        isChecked: Boolean,
        onClick: () -> Unit
    ) {
        val preference = RadioPreference(title, summary, isEnabled, isChecked, onClick)
        children.add(preference)
    }

    fun clickPref(
        title: ConversationSettingsText,
        summary: ConversationSettingsText? = null,
        icon: ConversationSettingsIcon? = null,
        iconEnd: ConversationSettingsIcon? = null,
        isEnabled: Boolean = true,
        onClick: () -> Unit,
        onLongClick: (() -> Boolean)? = null
    ) {
        val preference = ClickPreference(title, summary, icon, iconEnd, isEnabled, onClick, onLongClick)
        children.add(preference)
    }

    fun longClickPref(
        title: ConversationSettingsText,
        summary: ConversationSettingsText? = null,
        icon: ConversationSettingsIcon? = null,
        isEnabled: Boolean = true,
        onLongClick: () -> Unit
    ) {
        val preference = LongClickPreference(title, summary, icon, isEnabled, onLongClick)
        children.add(preference)
    }

    fun externalLinkPref(
        title: ConversationSettingsText,
        icon: ConversationSettingsIcon? = null,
        @StringRes linkId: Int
    ) {
        val preference = ExternalLinkPreference(title, icon, linkId)
        children.add(preference)
    }

    fun dividerPref() {
        val preference = DividerPreference()
        children.add(preference)
    }

    fun sectionHeaderPref(title: ConversationSettingsText) {
        val preference = SectionHeaderPreference(title)
        children.add(preference)
    }

    fun sectionHeaderPref(title: Int) {
        val preference = SectionHeaderPreference(ConversationSettingsText.from(title))
        children.add(preference)
    }

    fun noPadTextPref(title: ConversationSettingsText) {
        val preference = Text(title)
        children.add(Text.Model(preference))
    }

    fun space(@Px pixels: Int) {
        val preference = Space(pixels)
        children.add(Space.Model(preference))
    }

    fun primaryButton(
        text: ConversationSettingsText,
        isEnabled: Boolean = true,
        onClick: () -> Unit
    ) {
        val preference = Button.Model.Primary(text, null, isEnabled, onClick)
        children.add(preference)
    }

    fun tonalButton(
        text: ConversationSettingsText,
        isEnabled: Boolean = true,
        onClick: () -> Unit
    ) {
        val preference = Button.Model.Tonal(text, null, isEnabled, onClick)
        children.add(preference)
    }

    fun secondaryButtonNoOutline(
        text: ConversationSettingsText,
        icon: ConversationSettingsIcon? = null,
        isEnabled: Boolean = true,
        onClick: () -> Unit
    ) {
        val preference = Button.Model.SecondaryNoOutline(text, icon, isEnabled, onClick)
        children.add(preference)
    }

    fun textPref(
        title: ConversationSettingsText? = null,
        summary: ConversationSettingsText? = null
    ) {
        val preference = TextPreference(title, summary)
        children.add(preference)
    }

    fun toMappingModelList(): MappingModelList = MappingModelList().apply { addAll(children) }
}

abstract class PreferenceModel<T : PreferenceModel<T>>(
    open val title: ConversationSettingsText? = null,
    open val summary: ConversationSettingsText? = null,
    open val icon: ConversationSettingsIcon? = null,
    open val iconEnd: ConversationSettingsIcon? = null,
    open val isEnabled: Boolean = true,
) : MappingModel<T> {
    override fun areItemsTheSame(newItem: T): Boolean {
        return when {
            title != null -> title == newItem.title
            summary != null -> summary == newItem.summary
            else -> throw AssertionError("Could not determine equality of $newItem. Did you forget to override this method?")
        }
    }

    @CallSuper
    override fun areContentsTheSame(newItem: T): Boolean {
        return areItemsTheSame(newItem) &&
                newItem.summary == summary &&
                newItem.icon == icon &&
                newItem.isEnabled == isEnabled &&
                newItem.iconEnd == iconEnd
    }
}

class TextPreference(
    title: ConversationSettingsText?,
    summary: ConversationSettingsText?
) : PreferenceModel<TextPreference>(title = title, summary = summary)

class DividerPreference : PreferenceModel<DividerPreference>() {
    override fun areItemsTheSame(newItem: DividerPreference) = true
}

class RadioListPreference(
    override val title: ConversationSettingsText,
    override val icon: ConversationSettingsIcon? = null,
    override val isEnabled: Boolean,
    val dialogTitle: ConversationSettingsText = title,
    val listItems: Array<String>,
    val selected: Int,
    val onSelected: (Int) -> Unit,
    val confirmAction: Boolean = false
) : PreferenceModel<RadioListPreference>() {

    override fun areContentsTheSame(newItem: RadioListPreference): Boolean {
        return super.areContentsTheSame(newItem) && listItems.contentEquals(newItem.listItems) && selected == newItem.selected
    }
}

class MultiSelectListPreference(
    override val title: ConversationSettingsText,
    override val isEnabled: Boolean,
    val listItems: Array<String>,
    val selected: BooleanArray,
    val onSelected: (BooleanArray) -> Unit
) : PreferenceModel<MultiSelectListPreference>(title = title, isEnabled = isEnabled) {
    override fun areContentsTheSame(newItem: MultiSelectListPreference): Boolean {
        return super.areContentsTheSame(newItem) &&
                listItems.contentEquals(newItem.listItems) &&
                selected.contentEquals(newItem.selected)
    }
}

class SwitchPreference(
    override val title: ConversationSettingsText,
    override val summary: ConversationSettingsText? = null,
    override val icon: ConversationSettingsIcon? = null,
    override val isEnabled: Boolean,
    val isChecked: Boolean,
    val onClick: () -> Unit
) : PreferenceModel<SwitchPreference>() {
    override fun areContentsTheSame(newItem: SwitchPreference): Boolean {
        return super.areContentsTheSame(newItem) && isChecked == newItem.isChecked
    }
}

class RadioPreference(
    title: ConversationSettingsText,
    summary: ConversationSettingsText? = null,
    isEnabled: Boolean,
    val isChecked: Boolean,
    val onClick: () -> Unit
) : PreferenceModel<RadioPreference>(title = title, summary = summary, isEnabled = isEnabled) {
    override fun areContentsTheSame(newItem: RadioPreference): Boolean {
        return super.areContentsTheSame(newItem) && isChecked == newItem.isChecked
    }
}

class ClickPreference(
    override val title: ConversationSettingsText,
    override val summary: ConversationSettingsText? = null,
    override val icon: ConversationSettingsIcon? = null,
    override val iconEnd: ConversationSettingsIcon? = null,
    override val isEnabled: Boolean = true,
    val onClick: () -> Unit,
    val onLongClick: (() -> Boolean)? = null
) : PreferenceModel<ClickPreference>()

class LongClickPreference(
    override val title: ConversationSettingsText,
    override val summary: ConversationSettingsText? = null,
    override val icon: ConversationSettingsIcon? = null,
    override val isEnabled: Boolean = true,
    val onLongClick: () -> Unit
) : PreferenceModel<LongClickPreference>()

class ExternalLinkPreference(
    override val title: ConversationSettingsText,
    override val icon: ConversationSettingsIcon?,
    @StringRes val linkId: Int
) : PreferenceModel<ExternalLinkPreference>()

class SectionHeaderPreference(override val title: ConversationSettingsText) : PreferenceModel<SectionHeaderPreference>()
