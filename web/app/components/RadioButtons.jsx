import React from 'baret'
import Bacon from 'baconjs'
import { toObservable } from '../util/util'

const RadioButton = ({
    label,
    selected,
    onSelect
}) => (
    <label className="radio-option-container">
        {label}
        <input
            type="radio"
            className="radio-option"
            checked={selected}
            onChange={onSelect}
        />
        <span className="radio-checkmark" />
    </label>
)

const RadioButtons = ({
    options,
    keyValue = o => o.key,
    displayValue = o => o.value,
    equality = (option, selected) => keyValue(option) === selected,
    selected,
    onSelectionChanged
}) => {
    options = toObservable(options)
    selected = toObservable(selected)

    return (
        <div className="radio-select-container">
            {Bacon.combineWith(options, selected, (optionList, selectedItem) => (
                optionList.map((option, index) => (
                    <RadioButton
                        key={keyValue(option) || index}
                        label={displayValue(option) || 'NO DISPLAY VALUE'}
                        selected={equality(option, selectedItem)}
                        onSelect={() => onSelectionChanged(option)}
                    />
                ))
            ))}
        </div>
    )
}

export default RadioButtons
