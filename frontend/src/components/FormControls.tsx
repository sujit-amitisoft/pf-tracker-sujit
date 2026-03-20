import { useEffect, useRef, useState, type InputHTMLAttributes } from "react";

type SelectOption = { value: string; label: string };

type AppSelectProps = {
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder: string;
  className?: string;
  menuPlacement?: "down" | "up";
};

export function AppSelect({ value, onChange, options, placeholder, className, menuPlacement = "down" }: AppSelectProps) {
  const [open, setOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(() => Math.max(0, options.findIndex((option) => option.value === value)));
  const rootRef = useRef<HTMLDivElement | null>(null);
  const optionRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const activeLabel = options.find((option) => option.value === value)?.label ?? placeholder;

  useEffect(() => {
    setHighlightedIndex(Math.max(0, options.findIndex((option) => option.value === value)));
  }, [options, value]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  useEffect(() => {
    if (open) {
      optionRefs.current[highlightedIndex]?.focus();
    }
  }, [open, highlightedIndex]);

  const selectOption = (nextValue: string) => {
    onChange(nextValue);
    setOpen(false);
  };

  const handleTriggerKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setOpen(true);
      setHighlightedIndex((current) => {
        const baseIndex = current >= 0 ? current : 0;
        if (event.key === "ArrowDown") return Math.min(baseIndex + 1, options.length - 1);
        return Math.max(baseIndex - 1, 0);
      });
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      setOpen((current) => !current);
    }
  };

  const handleOptionKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setHighlightedIndex(Math.min(index + 1, options.length - 1));
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      setHighlightedIndex(Math.max(index - 1, 0));
      return;
    }

    if (event.key === "Home") {
      event.preventDefault();
      setHighlightedIndex(0);
      return;
    }

    if (event.key === "End") {
      event.preventDefault();
      setHighlightedIndex(options.length - 1);
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      selectOption(options[index].value);
      return;
    }

    if (event.key === "Tab") {
      setOpen(false);
    }
  };

  return (
    <div className={`filter-select app-select ${open ? "open" : ""} ${menuPlacement === "up" ? "menu-up" : ""} ${className ?? ""}`.trim()} ref={rootRef}>
      <button type="button" className="filter-select-trigger app-field-trigger" onClick={() => setOpen((current) => !current)} onKeyDown={handleTriggerKeyDown} aria-haspopup="listbox" aria-expanded={open}>
        <span>{activeLabel}</span>
        <span className="filter-select-chevron" />
      </button>
      {open ? (
        <div className="filter-select-menu app-select-menu" role="listbox">
          {options.map((option, index) => (
            <button
              key={option.value}
              ref={(node) => {
                optionRefs.current[index] = node;
              }}
              type="button"
              className={`filter-select-option ${option.value === value ? "active" : ""} ${index === highlightedIndex ? "keyboard-active" : ""}`}
              onClick={() => selectOption(option.value)}
              onMouseEnter={() => setHighlightedIndex(index)}
              onKeyDown={(event) => handleOptionKeyDown(event, index)}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

type AppDateFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & {
  className?: string;
};

export function AppDateField({ className, value, onChange, placeholder = "dd-mm-yyyy", onFocus, onBlur, ...rest }: AppDateFieldProps) {
  const [isFocused, setIsFocused] = useState(false);
  const hasValue = Boolean(value);

  return (
    <div className={`app-date-field app-date-field-native ${className ?? ""}`.trim()}>
      <input
        {...rest}
        type="date"
        value={value}
        onChange={onChange}
        onFocus={(event) => {
          setIsFocused(true);
          onFocus?.(event);
        }}
        onBlur={(event) => {
          setIsFocused(false);
          onBlur?.(event);
        }}
        className={`app-date-input app-date-input-native ${hasValue ? "has-value" : ""}`}
      />
      {!hasValue && !isFocused ? <span className="app-date-placeholder">{placeholder}</span> : null}
    </div>
  );
}
