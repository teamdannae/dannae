import pandas as pd
import numpy as np

# 파일 읽기
total_vocab_file = '단어_총집합_난이도_분류.xlsx'
total_vocab_data = pd.read_excel(total_vocab_file)

# 단어 길이 계산
total_vocab_data['word_length'] = total_vocab_data['word'].str.len()

# 빈도가 0이고 두 글자인 경우 난이도를 4로 설정하고, 두 글자가 아닌 경우 제거
total_vocab_data = total_vocab_data[~((total_vocab_data['빈도'] == 0) & (total_vocab_data['word_length'] != 2))]  # 두 글자가 아닌 경우 제거
total_vocab_data['난이도'] = np.where(
    (total_vocab_data['빈도'] == 0) & (total_vocab_data['word_length'] == 2),
    4,
    total_vocab_data['difficulty']  # 나머지 항목은 difficulty 값을 그대로 난이도로 설정
)

# 결과 저장
output_file_path = '단어_총집합_난이도_분류.xlsx'
total_vocab_data.to_excel(output_file_path, index=False)
