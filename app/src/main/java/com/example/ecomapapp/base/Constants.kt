package com.example.ecomapapp.base

import com.example.ecomapapp.model.Report

typealias Completion = () -> Unit
typealias StringCompletion = (String?) -> Unit
typealias ReportsCompletion = (List<Report>) -> Unit
