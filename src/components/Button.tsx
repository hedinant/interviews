import cn from "classnames";
import { useMemo } from "react";

interface ButtonProps {
  label: string;
  size?: string;
  color?: string;
  onClick?: () => void;
  disabled?: boolean;
  isLoading?: boolean;
}

export const Button: FC<ButtonProps> = ({
  size,
  label,
  color,
  onClick,
  disabled,
  isLoading,
}) => {
  const className = useMemo(
    () => cn([size && `size_${size}`, color && `color_${color}`]),
    [color, size]
  );

  return (
    <button
      onClick={onClick}
      className={className}
      disabled={disabled || isLoading}
    >
      {isLoading ? "Загрузка..." : label}
    </button>
  );
};
