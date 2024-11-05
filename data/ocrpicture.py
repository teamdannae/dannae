import pytesseract
from pdf2image import convert_from_path
import re

# Tesseract 경로 설정 (Windows에서 필요 시 경로 지정)
# pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

# PDF 파일 경로와 페이지 범위 지정
pdf_path = 'C:\\Users\\SSAFY\\Downloads\\현대_국어_사용_빈도_조사___한국어_학습용_어휘_선정을_위한_기초_조사.pdf'
start_page = 829
end_page = 1197

# 결과를 저장할 리스트
ocr_results = []

# PDF의 특정 페이지 범위를 이미지로 변환하여 OCR 수행
for page_number in range(start_page - 1, end_page):  # 0-index로 시작
    images = convert_from_path(pdf_path, first_page=page_number + 1, last_page=page_number + 1, poppler_path=r'C:\\Users\\SSAFY\\poppler-24.08.0\\Library\\bin')
    
    for image in images:
        # OCR 수행 (한국어 + 숫자만 포함한 텍스트 추출)
        text = pytesseract.image_to_string(image, lang='kor')

        # 한글과 숫자만 필터링 (정규 표현식 사용)
        filtered_text = re.findall(r'[가-힣0-9]+', text)
        
        # 결과를 문자열로 결합하여 저장
        filtered_text = ' '.join(filtered_text)
        ocr_results.append(filtered_text)

# 결과 확인
for i, result in enumerate(ocr_results, start=start_page):
    print(f"Page {i} OCR Result:")
    print(result)
    print("-" * 50)
