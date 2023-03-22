git-proxy:
	git config --global http.proxy http://127.0.0.1:7890
	git config --global https.proxy https://127.0.0.1:7890
	git config --global -l
git-unproxy:
	git config --global --unset http.proxy
	git config --global --unset https.proxy
	git config --global -l
# git提交信息
msg="auto"
git-push:
	echo msg
	git add .
	git commit -m msg
	git push origin master
