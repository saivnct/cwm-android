package com.lgt.cwm.activity.home.fragments.conversation

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.fragments.conversation.adapter.CreateGroupContactSelectionAdapter
import com.lgt.cwm.databinding.FragmentCreateGroupBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.ui.contact.ContactChip
import com.lgt.cwm.util.Constants.MAXIMUM_GROUP_MEMBER
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.ViewUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_create_group.*
import kotlinx.android.synthetic.main.fragment_create_group.recycler_view
import kotlinx.android.synthetic.main.fragment_create_group.swipe_refresh
import javax.inject.Inject

@AndroidEntryPoint
class CreateGroupFragment : Fragment() {
    private val TAG = CreateGroupFragment::class.simpleName.toString()

    companion object {
        private const val CHIP_GROUP_EMPTY_CHILD_COUNT = 1
        private const val CHIP_GROUP_REVEAL_DURATION_MS = 150
    }

    @Inject
    lateinit var debugConfig: DebugConfig

    private val createGroupViewModel: CreateGroupViewModel by viewModels()

    @Inject
    lateinit var createGroupContactSelectionAdapter: CreateGroupContactSelectionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentCreateGroupBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_create_group, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.createGroupViewModel = createGroupViewModel
        binding.createGroupContactSelectionAdapter = createGroupContactSelectionAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")

        initializeView()

        createGroupContactSelectionAdapter.setOnItemClickListener(object : CreateGroupContactSelectionAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: Contact, position: Int, selected: Boolean) {
                //TODO 1: create selectedContact attach to Contact Chip UI model
                //SelectedContact: model for chip UI
                if (selected) {
                    //create chip UI attach object selectedContact
                    addChipForContact(item)
                    createGroupViewModel.addSelectedPhoneFull(item.standardizedPhoneNumber)
                }else{
                    //remove chip UI (find by selectedContact)
                    removeChipForContact(item)
                    createGroupViewModel.removeSelectedPhoneFull(item.standardizedPhoneNumber)
                }

            }
        })



        buttonNext.setOnClickListener {
            val action = CreateGroupFragmentDirections.actionCreateGroupFragmentToAddGroupDetailsFragment(listPhoneFull = createGroupViewModel.selectedItems.toTypedArray())
            findNavController().navigate(action)
        }

        initObserver()
    }

    private fun initializeView() {
        swipe_refresh.isNestedScrollingEnabled = false
        swipe_refresh.isEnabled = false
    }

    fun initObserver(){
        createGroupViewModel.allOTTContact.observe(viewLifecycleOwner) { contacts ->
            createGroupContactSelectionAdapter.setItems(contacts)
        }

        createGroupViewModel.selectedItemsLiveData.observe(viewLifecycleOwner) { selectedItems ->
            if (selectedItems.isNotEmpty()){
                buttonNext.visibility = View.VISIBLE
            }else{
                buttonNext.visibility = View.GONE
            }
        }

    }


    private fun smoothScrollChipsToEnd() {
        val x = if (ViewUtil.isLtr(chipGroupScrollContainer)) chipGroup.width else 0
        chipGroupScrollContainer.smoothScrollTo(x, 0)
    }

    private fun setChipGroupVisibility(visibility: Int) {

        val transition = AutoTransition()
        transition.duration = CHIP_GROUP_REVEAL_DURATION_MS.toLong()
        transition.excludeChildren(recycler_view, true)
        transition.excludeTarget(recycler_view, true)
        TransitionManager.beginDelayedTransition(container, transition)
        val constraintSet = ConstraintSet()
        constraintSet.clone(container)
        constraintSet.setVisibility(R.id.chipGroupScrollContainer, visibility)
        constraintSet.applyTo(container)
    }

    private fun addChipForContact(selectedContact: Contact) {
        //model display UI
        val chip = ContactChip(ContextThemeWrapper(requireContext(), R.style.ContactSelectionChip))
        if (getChipCount() == 0) {
            setChipGroupVisibility(ConstraintSet.VISIBLE)
        }
        chip.text = selectedContact.name
        //attach to Chip UI
        chip.contact = selectedContact
        chip.isCloseIconVisible = true
        chip.setAvatar(selectedContact)

        chip.setOnCloseIconClickListener {
            //unselect contact from list
//            debugConfig.log(TAG, "chip on close")
            //remove chip UI (find by selectedContact)
            removeChipForContact(selectedContact)
            createGroupViewModel.removeSelectedPhoneFull(selectedContact.standardizedPhoneNumber)
            createGroupContactSelectionAdapter.removeSelectedItem(selectedContact.id)
        }

        chipGroup.layoutTransition.addTransitionListener(object : LayoutTransition.TransitionListener {
            override fun startTransition(transition: LayoutTransition, container: ViewGroup, view: View, transitionType: Int) {}

            override fun endTransition(transition: LayoutTransition, container: ViewGroup, view: View, transitionType: Int) {
                if (getView() == null || !requireView().isAttachedToWindow) {
//                    debugConfig.log(TAG, "Fragment's view was detached before the animation completed.")
                    return
                }

                if (view === chip && transitionType == LayoutTransition.APPEARING) {
                    chipGroup.layoutTransition.removeTransitionListener(this)
                    chipGroup.post { smoothScrollChipsToEnd() }
                }
            }
        })

        addChip(chip)
    }

    private fun addChip(chip: ContactChip) {
        chipGroup.addView(chip)

        if (chipGroup.childCount > MAXIMUM_GROUP_MEMBER) {
            showGroupLimitMessage(requireContext())
        }
    }

    private fun getChipCount(): Int {
        val count = chipGroup.childCount - CHIP_GROUP_EMPTY_CHILD_COUNT
        if (count < 0) throw AssertionError()
        return count
    }

    private fun removeChipForContact(contact: Contact) {
//        debugConfig.log(TAG, "removeChipForContact contact ${contact.id}")

        val iterator = chipGroup.iterator()
        while (iterator.hasNext()){
            val v = iterator.next()
            if (v is ContactChip && contact.id.equals(v.contact?.id)) {
                iterator.remove()
            }
        }

        if (getChipCount() == 0) {
            setChipGroupVisibility(ConstraintSet.GONE)
        }
    }

    private fun showGroupLimitMessage(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Maximum group size reached")
            .setMessage("Groups can have a maximum of ${MAXIMUM_GROUP_MEMBER} members.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}