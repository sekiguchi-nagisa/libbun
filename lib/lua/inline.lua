## @main
main()

## @indexOf
function libbun_indexOf(str, sub)
    local index = string.find(str, sub, 1, true)
    if index == nil then
        return -1
    end
    return index - 1
end

## @try
function libbun_try(functions)
	local status, exception = pcall(functions["try"])
	if not status then
		if functions["catch"] then
			functions["catch"](exception)
		end
	end
	if functions["finally"] then
		functions["finally"]()
	end
end
