import re

# txt 파일 읽기
with open('4단계.txt', 'r', encoding='utf-8') as file:
    lines = file.readlines()

# 한글만 남기고 정제하는 함수
def clean_korean(text):
    # 특수 문자 '-'와 '^' 제거
    text = re.sub(r'[-^]', '', text)
    
    # 한글만 남기고 나머지는 제거한 후, 단어별로 분리
    words = re.findall(r'[가-힣]+', text)
    return words

# 정제된 결과를 한 줄에 한 단어씩 저장할 집합(set) 사용
cleaned_words = set()
for line in lines:
    cleaned_words.update(clean_korean(line))

# 중복 제거된 단어를 리스트로 변환하고 정렬
cleaned_words = sorted(cleaned_words)

# 정제된 결과 확인
for word in cleaned_words:
    print(word)

# 결과를 다른 파일에 저장하려면
with open('cleaned_file4.txt', 'w', encoding='utf-8') as file:
    for word in cleaned_words:
        file.write(word + '\n')
