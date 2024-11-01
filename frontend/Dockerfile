# 1. Node.js 20.15.1 버전을 사용한 베이스 이미지 선택
FROM node:20.15.1-alpine

# 2. 작업 디렉토리 생성
WORKDIR /app

# 3. package.json과 package-lock.json을 복사하고 의존성 설치
COPY package*.json ./
RUN npm install

# 4. Next.js 소스 파일 복사
COPY . .

# 5. Next.js 빌드
RUN npm run build

# 6. 포트 노출
EXPOSE 3000

# 7. Next.js 앱 실행
CMD ["npm", "start"]