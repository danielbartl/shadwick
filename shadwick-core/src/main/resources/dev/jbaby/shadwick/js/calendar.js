/* Calendar/date picker adapted from Shadleaf (Apache-2.0, see NOTICE). */
(function(){
'use strict';

function hide(popup){if(popup.matches(':popover-open'))popup.hidePopover();}
function show(popup,source){if(popup.matches(':popover-open'))return;try{popup.showPopover({source});}catch(error){if(error instanceof TypeError)popup.showPopover();else throw error;}}
const WEEKDAYS = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
const YEARS_BACK = 100;
const YEARS_AHEAD = 10;
let instances = 0;

const calendarStates=new Map(), pickerStates=new Map();
const Shadwick=window.Shadwick=window.Shadwick||{};
function listenFor(cleanups){return(node,type,handler,options)=>{node.addEventListener(type,handler,options);cleanups.push(()=>node.removeEventListener(type,handler,options));};}
function inputsOf(root){return Array.from(root.querySelectorAll('input[type="date"]')).filter(input=>input.closest('.sw-calendar')===root);}
let ajaxRangeSerializationInstalled=false;
function installRangeSerialization(){
  if(ajaxRangeSerializationInstalled||!window.Wicket?.Event?.subscribe)return;
  ajaxRangeSerializationInstalled=true;
  Wicket.Event.subscribe('/ajax/call/init',(event,attributes)=>{
    const component=attributes.c&&document.getElementById(attributes.c);
    if(component?.matches('.sw-calendar-inputs'))attributes.sr=true;
  });
}
document.addEventListener('DOMContentLoaded',installRangeSerialization);
function scan(){
  installRangeSerialization();
  calendarStates.forEach((state,root)=>{const current=inputsOf(root);if(!root.isConnected||current.length!==state.inputs.length||current.some((input,index)=>input!==state.inputs[index])){state.cleanups.forEach(off=>off());calendarStates.delete(root);}});
  pickerStates.forEach((state,root)=>{const current=inputsOf(state.calendar);if(!root.isConnected||root.querySelector('.sw-calendar')!==state.calendar||current.length!==state.inputs.length||current.some((input,index)=>input!==state.inputs[index])){state.cleanups.forEach(off=>off());pickerStates.delete(root);}});
  document.querySelectorAll('.sw-date-picker').forEach(root=>{root.toggleAttribute('data-native',!HTMLElement.prototype.showPopover);});
  document.querySelectorAll('.sw-calendar').forEach(root=>{
    if(root.closest('.sw-date-picker[data-native]'))return;
    if(calendarStates.has(root))return;
    const inputs=inputsOf(root);if(inputs.length<1||inputs.length>2||root.querySelector('.sw-calendar-inputs')&&inputs.length!==2)return;
    const template=document.createElement('template');template.innerHTML='<div class="sw-calendar-nav"><button type="button" class="sw-calendar-nav-button sw-calendar-previous"><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m15 18-6-6 6-6"/></svg></button><button type="button" class="sw-calendar-nav-button sw-calendar-next"><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m9 18 6-6-6-6"/></svg></button></div><label class="sw-calendar-dropdown" data-unit="month"><span class="sw-calendar-dropdown-label"><span></span><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m6 9 6 6 6-6"/></svg></span><select class="sw-calendar-select"></select></label><label class="sw-calendar-dropdown" data-unit="year"><span class="sw-calendar-dropdown-label"><span></span><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m6 9 6 6 6-6"/></svg></span><select class="sw-calendar-select"></select></label><span class="sw-calendar-week-number-label"></span>';
    template.content.querySelector('.sw-calendar-previous').setAttribute('aria-label',root.dataset.previous||'Go to the previous month');
    template.content.querySelector('.sw-calendar-next').setAttribute('aria-label',root.dataset.next||'Go to the next month');
    template.content.querySelector('[data-unit=month] select').setAttribute('aria-label',root.dataset.month||'Month');
    template.content.querySelector('[data-unit=year] select').setAttribute('aria-label',root.dataset.year||'Year');
    template.content.querySelector('.sw-calendar-week-number-label').textContent=root.dataset.weekNumber||'Week';
    const state={inputs,cleanups:[]};calendarStates.set(root,state);setUpCalendar(root,template,listenFor(state.cleanups),state.cleanups,state);
  });
  if(HTMLElement.prototype.showPopover)document.querySelectorAll('.sw-date-picker').forEach(root=>{
    if(pickerStates.has(root))return;
    const calendar=root.querySelector('.sw-calendar');if(!calendar)return;
    const inputs=inputsOf(calendar);
    // Ajax replacement can temporarily leave a root without its native input or trigger.
    // Wait for the remaining children before registering an instance; the observer retries.
    if(inputs.length<1||inputs.length>2||calendar.querySelector('.sw-calendar-inputs')&&inputs.length!==2||!root.querySelector(':scope > .sw-date-picker-trigger .sw-date-picker-value')||!root.querySelector(':scope > .sw-date-picker-content'))return;
    const state={calendar,inputs,cleanups:[]};pickerStates.set(root,state);setUpPicker(root,listenFor(state.cleanups),state.cleanups,state);
  });
}
Shadwick.enhanceCalendars=scan;
new MutationObserver(records=>{
  if(records.some(r=>r.type==='childList'))scan();
  if(records.some(r=>r.type==='attributes'&&r.target.tagName==='FIELDSET')){calendarStates.forEach(state=>state.sync?.());pickerStates.forEach(state=>state.sync?.());}
}).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled']});
window.addEventListener('pageshow',()=>{scan();calendarStates.forEach(state=>state.sync?.());pickerStates.forEach(state=>state.sync?.());});

function setUpCalendar(root, template, listen, cleanups, handle) {
  const inputs = inputsOf(root);
  if (inputs.length < 1 || inputs.length > 2 || !template) {
    console.error('Shadwick: .sw-calendar needs one date input (a date) or two (a range), found %d.', inputs.length);
    return;
  }
  const range = inputs.length === 2;
  const id = `sw-calendar-${++instances}`;
  const config = readConfig(root);
  const formats = formatsFor(config.locale);

  // Markup from an earlier run, such as a page htmx restored from its history cache.
  root.querySelectorAll(':scope > .sw-calendar-months, :scope > .sw-calendar-status').forEach(element => element.remove());

  const body = element('div', 'sw-calendar-months');
  const nav = template.content.querySelector('.sw-calendar-nav').cloneNode(true);
  const previous = nav.querySelector('.sw-calendar-previous');
  const next = nav.querySelector('.sw-calendar-next');
  body.append(nav);
  const status = element('div', 'sw-calendar-status sw-calendar-sr-only');
  status.setAttribute('role', 'status');
  root.append(body, status);
  root.dataset.enhanced = 'true';
  for (const input of inputs) {
    input.tabIndex = -1;
    input.setAttribute('aria-hidden', 'true');
  }
  nameGroup(root, inputs, range, id);

  const state = {
    month: null,
    focus: null,
    hover: null,
    writing: false,
  };

  // --- values -------------------------------------------------------------------------------------------------------

  const valueOf = input => parseIso(input.value) ? input.value : '';
  const values = () => inputs.map(valueOf);
  const disabled = () => inputs.some(input => input.matches(':disabled'));
  const readonly = () => inputs.some(input => input.readOnly);
  const bounds = () => {
    const mins = inputs.map(input => input.min).filter(parseIso);
    const maxes = inputs.map(input => input.max).filter(parseIso);
    return {
      min: mins.length ? mins.sort().at(-1) : null,
      max: maxes.length ? maxes.sort()[0] : null,
    };
  };
  const monthBounds = () => {
    const {min, max} = bounds();
    const now = parseIso(root.dataset.today) || today();
    return {
      start: parseMonth(config.startMonth) ?? (min ? firstOfMonth(parseIso(min)) : dateAt(Math.max(1,now.getUTCFullYear() - YEARS_BACK), 0, 1)),
      end: parseMonth(config.endMonth) ?? (max ? firstOfMonth(parseIso(max)) : dateAt(Math.min(9999,now.getUTCFullYear() + YEARS_AHEAD), 11, 1)),
    };
  };
  const dayDisabled = date => {
    const {min, max} = bounds();
    const iso = toIso(date);
    return (min !== null && iso < min) || (max !== null && iso > max) || config.disabledDates(date);
  };

  /** The first month shown: the given one, moved so that every shown month lies within the bounds. */
  const clampMonth = month => {
    const {start, end} = monthBounds();
    const lastFirst = addMonths(end, 1 - config.numberOfMonths);
    if (month > lastFirst) {
      month = lastFirst;
    }
    return month < start ? start : month;
  };

  const anchor = () => values().find(value => value) ?? null;

  const write = next => {
    state.writing = true;
    try {
      const changed=next.map((value,index)=>({input:inputs[index],value})).filter(item=>item.input.value!==item.value);
      changed.forEach(item=>{item.input.value=item.value;});
      changed.forEach(item=>{item.input.dispatchEvent(new Event('input',{bubbles:true}));item.input.dispatchEvent(new Event('change',{bubbles:true}));});
    } finally {
      state.writing = false;
    }
  };

  const pick = iso => {
    if (disabled() || readonly()) {
      return;
    }
    if (!range) {
      const [value] = values();
      write([value === iso && !(inputs[0].required || inputs[0].getAttribute('aria-required') === 'true') ? '' : iso]);
    } else {
      const [from, to] = values();
      if (!from || to) {
        write([iso, '']);
      } else {
        if(config.disabledDates.intersects(from,iso)){status.textContent=root.dataset.unavailable||'Choose a range without unavailable dates.';return;}
        write(iso < from ? [iso, from] : [from, iso]);
      }
    }
    state.hover = null;
    render();
    root.dispatchEvent(new CustomEvent('sw-calendar-pick',{bubbles:true,detail:{complete:!range||values().every(Boolean)}}));
  };

  // --- drawing ------------------------------------------------------------------------------------------------------

  const render = () => {
    const focused = root.contains(document.activeElement) ? document.activeElement : null;
    const refocusSelect = focused?.matches('.sw-calendar-select')
        ? {month: monthIndexOf(focused), unit: focused.closest('.sw-calendar-dropdown').dataset.unit}
        : null;
    body.querySelectorAll(':scope > .sw-calendar-month').forEach(month => month.remove());

    const isDisabled = disabled();
    const {start, end} = monthBounds();
    previous.disabled = isDisabled || state.month <= start;
    next.disabled = isDisabled || addMonths(state.month, config.numberOfMonths - 1) >= end;

    for (let index = 0; index < config.numberOfMonths && addMonths(state.month,index)<=end; index++) {
      body.append(renderMonth(addMonths(state.month, index), index, isDisabled));
    }
    placeTabStop(isDisabled);
    if (refocusSelect) {
      body.querySelectorAll(':scope > .sw-calendar-month')[refocusSelect.month]
          ?.querySelector(`.sw-calendar-dropdown[data-unit="${refocusSelect.unit}"] > .sw-calendar-select`)?.focus();
    } else if (focused?.matches('.sw-calendar-day-button')) {
      dayButton(state.focus)?.focus();
    } else if (focused?.matches('.sw-calendar-nav-button') && focused.disabled) {
      // Reaching a month bound disables the button just activated. Keep focus inside the calendar.
      const navigation = previous.disabled ? next : previous;
      (navigation.disabled ? body.querySelector('.sw-calendar-day-button[tabindex="0"]') : navigation)?.focus();
    }
  };

  const monthIndexOf = node => Array.from(body.querySelectorAll(':scope > .sw-calendar-month'))
      .indexOf(node.closest('.sw-calendar-month'));

  const renderMonth = (month, index, isDisabled) => {
    const container = element('div', 'sw-calendar-month');
    const captionId = `${id}-caption-${index}`;
    const caption = element('div', 'sw-calendar-caption');
    caption.id = captionId;
    caption.append(renderCaption(month, isDisabled));
    container.append(caption);

    const grid = element('table', 'sw-calendar-grid');
    grid.setAttribute('role', 'grid');
    grid.setAttribute('aria-labelledby', captionId);
    grid.setAttribute('aria-multiselectable', String(range));
    if (readonly()) {
      grid.setAttribute('aria-readonly', 'true');
    }
    const head = element('thead');
    const headRow = element('tr', 'sw-calendar-weekdays');
    if (config.showWeekNumber) {
      // Text for screen readers: axe wants a column header with content, not only a name.
      const header = element('th', 'sw-calendar-week-number-header');
      header.scope = 'col';
      const label = element('span', 'sw-calendar-sr-only');
      label.textContent = weekNumberLabel();
      header.append(label);
      headRow.append(header);
    }
    for (const day of weekdayNames(formats, config.weekStartsOn)) {
      const header = element('th', 'sw-calendar-weekday');
      header.scope = 'col';
      header.setAttribute('aria-label', day.long);
      header.textContent = day.short;
      headRow.append(header);
    }
    head.append(headRow);

    const rows = element('tbody');
    const offset = (month.getUTCDay() - config.weekStartsOn + 7) % 7;
    const first = addDays(month, -offset);
    const weeks = Math.ceil((offset + daysInMonth(month)) / 7);
    const [from, to] = range ? values() : [null, null];
    const [single] = range ? [null] : values();
    const todayIso = toIso(parseIso(root.dataset.today) || today());
    for (let week = 0; week < weeks; week++) {
      const row = element('tr', 'sw-calendar-week');
      if (config.showWeekNumber) {
        const number = weekNumber(addDays(first, week * 7), config.weekStartsOn, config.firstWeekMinDays);
        const cell = element('th', 'sw-calendar-week-number');
        cell.scope = 'row';
        cell.setAttribute('aria-label', `${weekNumberLabel()} ${number}`);
        cell.textContent = String(number);
        row.append(cell);
      }
      for (let column = 0; column < 7; column++) {
        const date = addDays(first, week * 7 + column);
        row.append(renderDay(date, month, {from, to, single, todayIso, isDisabled}));
      }
      rows.append(row);
    }
    grid.append(head, rows);
    container.append(grid);
    return container;
  };

  const renderDay = (date, month, {from, to, single, todayIso, isDisabled}) => {
    const iso = toIso(date);
    const cell = element('td', 'sw-calendar-day');
    cell.setAttribute('role', 'gridcell');
    if (date.getUTCFullYear() < 1 || date.getUTCFullYear() > 9999) {
      cell.dataset.hidden = 'true';
      return cell;
    }
    const outside = date.getUTCMonth() !== month.getUTCMonth();
    if (outside && !config.showOutsideDays) {
      cell.dataset.hidden = 'true';
      return cell;
    }
    const flags = {};
    if (outside) {
      flags.outside = 'true';
    }
    if (iso === todayIso) {
      flags.today = 'true';
    }
    const unavailable = dayDisabled(date);
    if (unavailable) {
      flags.disabled = 'true';
    }
    let selected = false;
    if (range) {
      // While only the start is picked, the band runs to the day under the pointer.
      const end = to || (from && state.hover && !isDisabled ? state.hover : '');
      const [low, high] = end && end < from ? [end, from] : [from, end];
      if (from && iso === low) {
        flags.rangeStart = 'true';
      }
      if (from && (iso === high || (!high && iso === low))) {
        flags.rangeEnd = 'true';
      }
      if (low && high && iso > low && iso < high) {
        flags.rangeMiddle = 'true';
      }
      selected = !!from && (to ? iso >= from && iso <= to : iso === from);
    } else {
      selected = iso === single;
    }
    if (selected) {
      flags.selected = 'true';
    }
    Object.assign(cell.dataset, flags);
    cell.setAttribute('aria-selected', String(selected));

    const button = element('button', 'sw-calendar-day-button');
    button.type = 'button';
    Object.assign(button.dataset, flags);
    button.dataset.date = iso;
    button.tabIndex = -1;
    button.textContent = String(date.getUTCDate());
    button.setAttribute('aria-label', formats.day.format(date));
    if (flags.today) {
      button.setAttribute('aria-current', 'date');
    }
    if (isDisabled) {
      button.disabled = true;
    } else if (unavailable) {
      // Still takes the focus, so the arrow keys move across it.
      button.setAttribute('aria-disabled', 'true');
    }
    cell.append(button);
    return cell;
  };

  const renderCaption = (month, isDisabled) => {
    const layout = config.captionLayout;
    if (layout === 'label') {
      const label = element('span', 'sw-calendar-caption-label');
      label.textContent = formats.caption.format(month);
      return label;
    }
    const dropdowns = element('div', 'sw-calendar-dropdowns');
    const {start, end} = monthBounds();
    if (layout === 'dropdown' || layout === 'dropdown-months') {
      const options = [];
      for (let m = 0; m < 12; m++) {
        const first = dateAt(month.getUTCFullYear(), m, 1);
        options.push({value: String(m), text: formats.monthShort.format(first), disabled: first < start || first > end});
      }
      dropdowns.append(dropdown('month', options, String(month.getUTCMonth()), formats.monthShort.format(month), isDisabled));
    } else {
      dropdowns.append(captionText(formats.monthLong.format(month)));
    }
    if (layout === 'dropdown' || layout === 'dropdown-years') {
      const options = [];
      for (let year = start.getUTCFullYear(); year <= end.getUTCFullYear(); year++) {
        options.push({value: String(year), text: formats.year.format(dateAt(year, 0, 1)), disabled: false});
      }
      dropdowns.append(dropdown('year', options, String(month.getUTCFullYear()), formats.year.format(month), isDisabled));
    } else {
      dropdowns.append(captionText(formats.year.format(month)));
    }
    return dropdowns;
  };

  const captionText = text => {
    const label = element('span', 'sw-calendar-caption-label');
    label.textContent = text;
    return label;
  };

  const dropdown = (unit, options, value, text, isDisabled) => {
    const label = template.content.querySelector(`.sw-calendar-dropdown[data-unit="${unit}"]`).cloneNode(true);
    label.querySelector('.sw-calendar-dropdown-label > span').textContent = text;
    const select = label.querySelector('.sw-calendar-select');
    for (const option of options) {
      const choice = new Option(option.text, option.value, false, option.value === value);
      choice.disabled = option.disabled;
      select.append(choice);
    }
    select.disabled = isDisabled;
    return label;
  };

  const weekNumberLabel = () => template.content.querySelector('.sw-calendar-week-number-label')?.textContent ?? '';

  /** Exactly one day takes the focus: the one that last had it, else the selection, today or the first day. */
  const placeTabStop = isDisabled => {
    if (isDisabled) {
      return;
    }
    const own = ':not([data-outside])';
    const target = dayButton(state.focus)
        ?? body.querySelector(`.sw-calendar-day-button[data-selected]${own}`)
        ?? body.querySelector(`.sw-calendar-day-button[data-today]${own}`)
        ?? body.querySelector(`.sw-calendar-day-button${own}:not([aria-disabled])`)
        ?? body.querySelector(`.sw-calendar-day-button${own}`);
    if (target) {
      target.tabIndex = 0;
      state.focus = target.dataset.date;
    }
  };

  /** The button of a day, in its own month rather than as another month's outside day. */
  const dayButton = iso => iso
      ? body.querySelector(`.sw-calendar-day-button[data-date="${iso}"]:not([data-outside])`)
        ?? body.querySelector(`.sw-calendar-day-button[data-date="${iso}"]`)
      : null;

  // --- navigation ---------------------------------------------------------------------------------------------------

  const showMonth = (month, {announce = true} = {}) => {
    const next = clampMonth(month);
    if (state.month && next.getTime() === state.month.getTime()) {
      return;
    }
    state.month = next;
    render();
    if (announce) {
      status.textContent = Array.from(body.querySelectorAll(':scope > .sw-calendar-month'))
          .map(month => formats.caption.format(monthOf(month)))
          .join(', ');
    }
  };

  const monthOf = container => addMonths(state.month, Array.from(body.children).indexOf(container) - 1);

  /** Shows the months holding `iso`, unless they are already shown. */
  const reveal = iso => {
    const month = firstOfMonth(parseIso(iso));
    const last = addMonths(state.month, config.numberOfMonths - 1);
    if (month < state.month) {
      showMonth(month);
    } else if (month > last) {
      showMonth(addMonths(month, 1 - config.numberOfMonths));
    }
  };

  const moveFocus = date => {
    const {start, end} = monthBounds();
    const lastDay = addDays(addMonths(end, 1), -1);
    if (date < start) {
      date = start;
    } else if (date > lastDay) {
      date = lastDay;
    }
    state.focus = toIso(date);
    reveal(state.focus);
    body.querySelectorAll('.sw-calendar-day-button[tabindex="0"]').forEach(button => {
      button.tabIndex = -1;
    });
    const button = dayButton(state.focus);
    if (button) {
      button.tabIndex = 0;
      button.focus();
    }
  };

  listen(body, 'click', event => {
    const target = event.target instanceof Element ? event.target : null;
    if (target?.closest('.sw-calendar-previous')) {
      showMonth(addMonths(state.month, -1));
    } else if (target?.closest('.sw-calendar-next')) {
      showMonth(addMonths(state.month, 1));
    } else {
      const button = target?.closest('.sw-calendar-day-button');
      if (!button || button.disabled || button.hasAttribute('aria-disabled')) {
        return;
      }
      state.focus = button.dataset.date;
      pick(button.dataset.date);
    }
  });

  // The month and year selects are no part of the form's value: their events stay inside the calendar, so a form's
  // hx-trigger="change" or listeners do not see them.
  listen(body, 'input', event => {
    if (event.target instanceof Element && event.target.matches('.sw-calendar-select')) {
      event.stopPropagation();
    }
  });

  listen(body, 'change', event => {
    const select = event.target;
    if (!(select instanceof HTMLSelectElement) || !select.matches('.sw-calendar-select')) {
      return;
    }
    event.stopPropagation();
    const index = monthIndexOf(select);
    const shown = addMonths(state.month, index);
    const chosen = select.closest('.sw-calendar-dropdown').dataset.unit === 'month'
        ? dateAt(shown.getUTCFullYear(), Number(select.value), 1)
        : dateAt(Number(select.value), shown.getUTCMonth(), 1);
    showMonth(addMonths(chosen, -index));
  });

  listen(body, 'keydown', event => {
    const button = event.target instanceof Element ? event.target.closest('.sw-calendar-day-button') : null;
    if (!button || event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }
    const current = parseIso(button.dataset.date);
    const rtl = getComputedStyle(root).direction === 'rtl';
    const weekday = (current.getUTCDay() - config.weekStartsOn + 7) % 7;
    let target;
    switch (event.key) {
      case 'ArrowRight':
        target = addDays(current, rtl ? -1 : 1);
        break;
      case 'ArrowLeft':
        target = addDays(current, rtl ? 1 : -1);
        break;
      case 'ArrowDown':
        target = addDays(current, 7);
        break;
      case 'ArrowUp':
        target = addDays(current, -7);
        break;
      case 'Home':
        target = addDays(current, -weekday);
        break;
      case 'End':
        target = addDays(current, 6 - weekday);
        break;
      case 'PageDown':
        target = addMonthsKeepingDay(current, event.shiftKey ? 12 : 1);
        break;
      case 'PageUp':
        target = addMonthsKeepingDay(current, event.shiftKey ? -12 : -1);
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        if (!button.hasAttribute('aria-disabled')) {
          state.focus = button.dataset.date;
          pick(button.dataset.date);
        }
        return;
      default:
        return;
    }
    event.preventDefault();
    moveFocus(target);
  });

  listen(body, 'pointerover', event => {
    const button = event.target instanceof Element ? event.target.closest('.sw-calendar-day-button') : null;
    const [from, to] = values();
    const hover = button && !button.hasAttribute('aria-disabled') ? button.dataset.date : null;
    if (!range || !from || to || hover === state.hover || readonly()) {
      return;
    }
    state.hover = hover;
    render();
  });

  listen(body, 'pointerleave', () => {
    if (state.hover) {
      state.hover = null;
      render();
    }
  });

  // A value set by a script (that fires change, as for any input) shows its month.
  listen(root, 'change', event => {
    if (state.writing || !inputs.includes(event.target)) {
      return;
    }
    const value = anchor();
    if (value) {
      reveal(value);
    }
    render();
  });

  for (const input of inputs) {
    listen(input, 'focus', () => {
      if (!disabled()) {
        body.querySelector('.sw-calendar-day-button[tabindex="0"]')?.focus();
      }
    });
  }

  listen(root,'focusin',event=>{if(event.target.matches('.sw-calendar-inputs'))body.querySelector('.sw-calendar-day-button[tabindex="0"]')?.focus();});
  const form = inputs[0].form;
  if (form) {
    // The reset event comes before the form resets its controls.
    listen(form, 'reset', event => setTimeout(() => {
      if(event.defaultPrevented||!root.isConnected)return;
      state.hover = null;
      const value = anchor();
      state.focus = value;
      if (value) {
        reveal(value);
      }
      render();
    }));
  }

  // disabled, readonly, min, max and required can change after the page loaded.
  const sync=()=>{nameGroup(root,inputs,range,id);const descriptions=[...new Set(inputs.flatMap(input=>(input.getAttribute('aria-describedby')||'').split(/\s+/).filter(Boolean)))];if(descriptions.length)root.setAttribute('aria-describedby',descriptions.join(' '));else root.removeAttribute('aria-describedby');root.setAttribute('aria-invalid',String(inputs.some(input=>input.getAttribute('aria-invalid')==='true')));render();};
  const observer = new MutationObserver(sync);
  for (const input of inputs) {
    observer.observe(input, {attributes: true, attributeFilter: ['disabled', 'readonly', 'min', 'max', 'required','aria-required','aria-invalid','aria-describedby','aria-label','aria-labelledby']});
  }
  const fieldset = root.closest('fieldset');
  if (fieldset) {
    observer.observe(fieldset, {attributes: true, attributeFilter: ['disabled']});
  }
  cleanups.push(() => observer.disconnect());

  /** The months and the day that takes the focus as the calendar starts with: those of its value, else today's. */
  const showStart = () => {
    state.hover = null;
    const value = anchor();
    state.focus = value;
    showMonth(parseMonth(config.defaultMonth) ?? firstOfMonth(value ? parseIso(value) : parseIso(root.dataset.today) || today()), {announce: false});
    render();
  };

  // sl:date-picker sends this when it opens the calendar again, after the focus may have wandered off.
  listen(root, 'sw-calendar-show-start', event => {
    if (event.target === root) {
      showStart();
    }
  });

  handle.sync=sync;handle.showStart=showStart;
  showStart();sync();
  if (inputs.includes(document.activeElement)) {
    body.querySelector('.sw-calendar-day-button[tabindex="0"]')?.focus();
  }
}

/**
 * The calendar is a group named like its single input: by the input's labels, aria-labelledby or aria-label. A range's
 * two inputs have names of their own (start, end); there the calendar keeps the attributes it was given.
 */
function nameGroup(root, inputs, range, id) {
  if (range || root.hasAttribute('aria-label') || root.hasAttribute('aria-labelledby')) {
    return;
  }
  const [input] = inputs;
  const labels = Array.from(input.labels ?? []);
  if (input.hasAttribute('aria-labelledby')) {
    root.setAttribute('aria-labelledby',input.getAttribute('aria-labelledby'));
  } else if(input.hasAttribute('aria-label')) {
    root.setAttribute('aria-label',input.getAttribute('aria-label'));
  } else if (labels.length > 0) {
    root.setAttribute('aria-labelledby', labels.map((label, index) => {
      label.id ||= `${id}-label-${index}`;
      return label.id;
    }).join(' '));
  } else if (input.hasAttribute('aria-labelledby')) {
    root.setAttribute('aria-labelledby', input.getAttribute('aria-labelledby'));
  } else if (input.hasAttribute('aria-label')) {
    root.setAttribute('aria-label', input.getAttribute('aria-label'));
  }
  if (input.hasAttribute('aria-describedby')) {
    root.setAttribute('aria-describedby', input.getAttribute('aria-describedby'));
  }
}

function readConfig(root) {
  const data = root.dataset;
  const number = (value, fallback) => Number.isFinite(Number.parseInt(value, 10)) ? Number.parseInt(value, 10) : fallback;
  return {
    locale: data.locale || document.documentElement.lang || undefined,
    captionLayout: data.caption || 'label',
    numberOfMonths: Math.min(12,Math.max(1, number(data.numberOfMonths, 1))),
    showOutsideDays: data.showOutsideDays !== 'false',
    showWeekNumber: data.showWeekNumber === 'true',
    weekStartsOn: ((number(data.weekStartsOn, 0) % 7) + 7) % 7,
    firstWeekMinDays: Math.min(7, Math.max(1, number(data.firstWeekMinDays, 1))),
    defaultMonth: data.defaultMonth,
    startMonth: data.startMonth,
    endMonth: data.endMonth,
    disabledDates: parseDisabledDates(data.disabledDates ?? ''),
  };
}

function formatsFor(locale) {
  const format = options => {
    try {
      return new Intl.DateTimeFormat(locale, {...options,timeZone:'UTC',calendar:'gregory'});
    } catch {
      return new Intl.DateTimeFormat(undefined, {...options,timeZone:'UTC',calendar:'gregory'});
    }
  };
  return {
    caption: format({month: 'long', year: 'numeric'}),
    monthLong: format({month: 'long'}),
    monthShort: format({month: 'short'}),
    year: format({year: 'numeric'}),
    day: format({dateStyle: 'full'}),
    weekdayShort: format({weekday: 'short'}),
    weekdayNarrow: format({weekday: 'narrow'}),
    weekdayLong: format({weekday: 'long'}),
  };
}

/**
 * The column headers, starting on `weekStartsOn`: two letters where those stay distinct ("Su Mo"), as
 * react-day-picker shows them, otherwise the locale's narrow names, as some languages' short names share a prefix.
 */
function weekdayNames(formats, weekStartsOn) {
  // 7 January 2024 is a Sunday.
  const days = Array.from({length: 7}, (_, index) => dateAt(2024, 0, 7 + weekStartsOn + index));
  const short = days.map(day => formats.weekdayShort.format(day).slice(0, 2));
  const names = new Set(short).size === 7 ? short : days.map(day => formats.weekdayNarrow.format(day));
  return days.map((day, index) => ({short: names[index], long: formats.weekdayLong.format(day)}));
}

/** A date (2026-12-25), a range (2026-12-24..2026-12-31) or a day of the week (sat), comma-separated. */
function parseDisabledDates(spec){
  const rules=[];
  for(const token of spec.split(',').map(s=>s.trim().toLowerCase()).filter(Boolean)){
    const weekday=WEEKDAYS.indexOf(token.slice(0,3));const parts=token.split('..');
    if(weekday>=0&&(token===WEEKDAYS[weekday]||token===['sunday','monday','tuesday','wednesday','thursday','friday','saturday'][weekday]))rules.push({weekday});
    else if(parts.length<=2&&parseIso(parts[0])&&parseIso(parts[1]||parts[0])&&(parts[1]||parts[0])>=parts[0])rules.push({from:parts[0],to:parts[1]||parts[0]});
    else throw new Error('Invalid disabled date rule: '+token);
  }
  const matches=date=>rules.some(rule=>rule.weekday!==undefined?date.getUTCDay()===rule.weekday:toIso(date)>=rule.from&&toIso(date)<=rule.to);
  matches.intersects=(first,last)=>{const [from,to]=first<=last?[first,last]:[last,first];return rules.some(rule=>{
    if(rule.weekday===undefined)return to>=rule.from&&from<=rule.to;
    const date=parseIso(from);for(let i=0;i<=Math.min(6,daysBetween(date,parseIso(to)));i++)if(addDays(date,i).getUTCDay()===rule.weekday)return true;return false;
  });};return matches;
}

/**
 * The week number of the week starting on `weekStart`, counted as Java's WeekFields: week 1 is the first week with at
 * least `minDays` days of the new year (ISO: weeks start on Monday, 4 days).
 */
function weekNumber(weekStart, weekStartsOn, minDays) {
  const firstWeek = year => {
    const january = dateAt(year, 0, 1);
    const offset = (january.getUTCDay() - weekStartsOn + 7) % 7;
    const start = addDays(january, -offset);
    return 7 - offset >= minDays ? start : addDays(start, 7);
  };
  let year = addDays(weekStart, 6).getUTCFullYear();
  let start = firstWeek(year);
  if (weekStart < start) {
    start = firstWeek(--year);
  }
  return Math.round(daysBetween(start, weekStart) / 7) + 1;
}

function element(tag, className) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  return node;
}

function pad(number) {
  return String(number).padStart(2, '0');
}

function toIso(date) {
  return `${String(date.getUTCFullYear()).padStart(4, '0')}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

/** A local date from an ISO date (2026-10-23), or null for anything else, an impossible date included. */
function parseIso(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? '');
  if (!match || Number(match[1])<1) {
    return null;
  }
  const date = dateAt(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  date.setUTCFullYear(Number(match[1]));
  return toIso(date) === value ? date : null;
}

function parseMonth(value) {
  return parseIso(value ? `${value.slice(0, 7)}-01` : null);
}

function today(){const now=new Date();return dateAt(now.getFullYear(),now.getMonth(),now.getDate());}
function dateAt(year,month,day){const date=new Date(0);date.setUTCHours(12,0,0,0);date.setUTCFullYear(year,month,day);return date;}

function firstOfMonth(date) {
  return dateAt(date.getUTCFullYear(), date.getUTCMonth(), 1);
}

function addDays(date, days) {
  return dateAt(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate() + days);
}

function addMonths(date, months) {
  return dateAt(date.getUTCFullYear(), date.getUTCMonth() + months, 1);
}

/** The same day `months` later, or the last day of that month when it is shorter (31 January + 1 month). */
function addMonthsKeepingDay(date, months) {
  const month = addMonths(date, months);
  return dateAt(month.getUTCFullYear(), month.getUTCMonth(), Math.min(date.getUTCDate(), daysInMonth(month)));
}

function daysInMonth(month) {
  return dateAt(month.getUTCFullYear(), month.getUTCMonth() + 1, 0).getUTCDate();
}

/** Whole days from `a` to `b`, counted on the calendar, so a change to or from summer time makes no difference. */
function daysBetween(a,b){return Math.round((b.getTime()-a.getTime())/86400000);}

function setUpPicker(root, listen, cleanups, handle) {
  const trigger = root.querySelector(':scope > .sw-date-picker-trigger');
  const popup = root.querySelector(':scope > .sw-date-picker-content');
  const calendar = popup?.querySelector(':scope > .sw-calendar');
  const inputs = calendar ? inputsOf(calendar) : [];
  const label = trigger?.querySelector('.sw-date-picker-value');
  if (!trigger || !label || inputs.length < 1 || inputs.length > 2) {
    console.error('Shadwick: .sw-date-picker needs its button and a calendar with one date input (a date) or two (a '
        + 'range), found %d.', inputs.length);
    return;
  }
  const range = inputs.length === 2;
  const id = `sw-date-picker-${++instances}`;
  const format = formatFor(calendar.dataset.locale || document.documentElement.lang || undefined,
      root.dataset.dateFormat || 'long');
  const placeholder = root.dataset.placeholder ?? '';
  const rangeFormat = root.dataset.rangeFormat || '{0} – {1}';

  const values = () => inputs.map(input => parseIso(input.value) ? input.value : '');
  const complete = () => values().every(value => value);

  const text = () => {
    const [from, to] = values();
    if (!from) return to ? format.format(parseIso(to)) : null;
    const start = format.format(parseIso(from));
    return range && to ? rangeFormat.replace('{0}', start).replace('{1}', format.format(parseIso(to))) : start;
  };

  const update = () => {
    const shown = text();
    label.textContent = shown ?? placeholder;
    if (shown === null) {
      trigger.dataset.empty = 'true';
    } else {
      delete trigger.dataset.empty;
    }
    mirrorField();
  };

  /** The field's state, which the server put on the inputs: aria-invalid and aria-describedby. */
  const mirrorField = () => {
    if (inputs.some(input => input.getAttribute('aria-invalid') === 'true')) {
      trigger.setAttribute('aria-invalid', 'true');
    } else {
      trigger.removeAttribute('aria-invalid');
    }
    const describedBy = [...new Set(inputs.flatMap(input => (input.getAttribute('aria-describedby') ?? '')
        .split(/\s+/).filter(Boolean)))];
    if (describedBy.length > 0) trigger.setAttribute('aria-describedby', describedBy.join(' '));
    else trigger.removeAttribute('aria-describedby');

  };

  const ensureId = (element, suffix) => {
    element.id ||= `${id}-${suffix}`;
    return element.id;
  };

  // The button's name: what names the calendar, then the date it shows. aria-labelledby takes an element's
  // aria-label as its text, so a hidden input or calendar can lend its own.
  const nameButton = () => {
    const source = range ? calendar : inputs[0];
    const labels = range ? [] : Array.from(inputs[0].labels ?? []);
    let names;
    if(source.hasAttribute('aria-labelledby'))names=source.getAttribute('aria-labelledby').split(/\s+/).filter(Boolean);
    else if(source.hasAttribute('aria-label'))names=[ensureId(source,'name')];
    else if (labels.length > 0) {
      names = labels.map((element, index) => ensureId(element, `label-${index}`));
    } else if (source.hasAttribute('aria-labelledby')) {
      names = source.getAttribute('aria-labelledby').split(/\s+/).filter(Boolean);
    } else if (source.hasAttribute('aria-label')) {
      names = [ensureId(source, 'name')];
    } else {
      return;
    }
    trigger.setAttribute('aria-labelledby', [...names, ensureId(label, 'value')].join(' '));
  };

  const updateDisabled = () => {
    trigger.disabled = inputs.some(input => input.matches(':disabled'));
  };

  // Opening: the day that takes the focus gets autofocus, which the popover gives the focus to as it shows.
  listen(popup, 'beforetoggle', event => {
    if (event.newState !== 'open') {
      return;
    }
    calendar.dispatchEvent(new CustomEvent('sw-calendar-show-start'));
    calendar.querySelectorAll('.sw-calendar-day-button[autofocus]').forEach(button => button.removeAttribute('autofocus'));
    calendar.querySelector('.sw-calendar-day-button[tabindex="0"]')?.setAttribute('autofocus', '');
  });

  // A pick writes the inputs one after the other (a new range clears the end after setting the start): decide once
  // all of them are written.
  let closing = 0;
  listen(calendar, 'sw-calendar-pick', event => {
    if (event.target !== calendar) {
      return;
    }
    update();
    clearTimeout(closing);
    closing = setTimeout(() => {
      if (!popup.matches(':popover-open') || !complete()) {
        return;
      }
      const hadFocus = popup.contains(document.activeElement);
      hide(popup);
      if (hadFocus && trigger.isConnected && !trigger.contains(document.activeElement)) {
        trigger.focus();
      }
    });
  });
  cleanups.push(() => clearTimeout(closing));

  listen(calendar,'change',event=>{if(inputs.includes(event.target))update();});
  for(const input of inputs)listen(input,'focus',()=>{if(!popup.matches(':popover-open'))trigger.focus();});
  // invalid does not bubble.
  listen(calendar, 'invalid', event => {
    if (!inputs.includes(event.target)) {
      return;
    }
    trigger.setAttribute('aria-invalid', 'true');
    show(popup, trigger);
  }, true);

  {
    for (const element of inputs[0].labels ?? []) {
      listen(element, 'click', event => {
        event.preventDefault();
        if (!trigger.disabled) {
          trigger.focus();
        }
      });
    }
  }

  const form = inputs[0].form;
  if (form) {
    // The reset event comes before the form resets its controls.
    listen(form,'reset',event=>setTimeout(()=>{if(!event.defaultPrevented&&root.isConnected)update();}));
  }

  const sync=()=>{update();nameButton();updateDisabled();};
  const observer = new MutationObserver(sync);
  for (const input of inputs) {
    observer.observe(input, {attributes: true, attributeFilter: ['disabled','aria-invalid','aria-describedby','aria-required','aria-label','aria-labelledby']});
  }
  const fieldset = root.closest('fieldset');
  if (fieldset) {
    observer.observe(fieldset, {attributes: true, attributeFilter: ['disabled','aria-invalid','aria-describedby','aria-required','aria-label','aria-labelledby']});
  }
  cleanups.push(() => observer.disconnect());

  handle.sync=sync;
  sync();
}

function formatFor(locale, dateStyle) {
  try {
    return new Intl.DateTimeFormat(locale, {dateStyle,timeZone:'UTC',calendar:'gregory'});
  } catch {
    return new Intl.DateTimeFormat(undefined, {dateStyle:'long',timeZone:'UTC',calendar:'gregory'});
  }
}

scan();
})();
