import styles from "./common.module.scss";

interface RadioProps {
  selectedIndex: number;
  values: string[];
  onChangeEvent: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const Radio: React.FC<RadioProps> = ({ selectedIndex = 0, values, onChangeEvent }: RadioProps) => {
  return (
    <fieldset className={styles.radioContainer}>
      {values.map((value, index) => (
        <label key={index} className={styles.radio}>
          <input
            type="radio"
            name="customRadio"
            value={index}
            checked={selectedIndex === index}
            onChange={onChangeEvent}
            className={styles.radioButton}
          />
          <span className={styles.radioText}>{value}</span>
        </label>
      ))}
    </fieldset>
  );
};

export default Radio;
